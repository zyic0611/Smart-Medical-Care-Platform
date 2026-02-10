package com.yicheng.modules.elderly.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.common.RedisConstants;
import com.yicheng.common.Result;
import com.yicheng.common.UserContext;
import com.yicheng.common.enums.ResultCode;
import com.yicheng.modules.bed.pojo.entity.BedEntity;
import com.yicheng.modules.bed.service.IBedService;
import com.yicheng.modules.elderly.pojo.dto.ElderlyDTO;
import com.yicheng.modules.elderly.pojo.dto.ElderlyUpdateDTO;
import com.yicheng.modules.elderly.pojo.entity.ElderlyEntity;
import com.yicheng.common.exception.CustomException;
import com.yicheng.modules.bed.mapper.BedMapper;
import com.yicheng.modules.elderly.mapper.ElderlyMapper;
import com.yicheng.modules.elderly.pojo.vo.ElderlyVO;
import com.yicheng.modules.employee.pojo.entity.EmployeeEntity;
import com.yicheng.modules.employee.service.IEmployeeService;
import com.yicheng.modules.sysdict.service.ISysDictService;
import com.yicheng.modules.user.entity.SysUser;
import com.yicheng.utils.CacheClient;
import com.yicheng.utils.Func;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElderlyServiceImpl extends ServiceImpl<ElderlyMapper, ElderlyEntity> implements IService<ElderlyEntity>, IElderlyService {

    //操作表数据
    private final ElderlyMapper elderlyMapper;
    private final BedMapper bedMapper;

    //字典
    private final ISysDictService sysDictService;

    //外键
    private final IBedService bedService;
    private final IEmployeeService IEmployeeService;

    private final CacheClient cacheClient;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;


    /**
     * 1. 分页查询
     */
    @Override
    public IPage<ElderlyVO> selectElderlyPage(Integer pageNum, Integer pageSize, ElderlyVO queryParam) {
        Page<ElderlyVO> page = new Page<>(pageNum, pageSize);
        IPage<ElderlyVO> result = elderlyMapper.selectElderlyPage(page, queryParam);
        //包装
        this.setParaStr(result.getRecords());
        return result;
    }



    /**
     * 2. 新增老人 (带床位联动) 分布式锁
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean addElderlyWithBed(ElderlyDTO elderlyDTO) {
        //1 数据默认初始化
        // 默认入住时间为当前时间
        if (elderlyDTO.getCheckInDate() == null) elderlyDTO.setCheckInDate(LocalDate.now());
        // 默认健康
        if(elderlyDTO.getNurseId() == null) elderlyDTO.setNurseId(0);
        // 类型转换
        ElderlyEntity elderlyEntity = BeanUtil.copyProperties(elderlyDTO, ElderlyEntity.class);

        //2 如果不需要床位 则不涉及多表关联 直接保存返回 不加锁 提升性能
        // 2. 修正 NPE 风险：先获取原始 ID 判断
        Integer bedId = elderlyDTO.getBedId();
        if (bedId == null) {
            return this.save(elderlyEntity);
        }

        //3 开启Redisson锁
        String lockKey=RedisConstants.LOCK_BED_ASSIGN_KEY+bedId;
        RLock lock=redissonClient.getLock(lockKey);
        boolean isLock=false;
        try{
            isLock=lock.tryLock(0,10,TimeUnit.SECONDS);
            if(!isLock){
                //上锁失败 则该床位正在被分配中
                throw new CustomException(ResultCode.LOCK_FAIL);
            }
            //即使拿到了redisson锁 也要通过数据库影响行数确认
            //防止逻辑漏洞或者锁超时 影响的并发问题
            boolean updateSuccess=this.updateBedStatus(elderlyEntity.getBedId(),1);
            if(!updateSuccess){
                throw new CustomException(ResultCode.BED_OCCUPIED);
            }
            return this.save(elderlyEntity);

        }catch (RuntimeException e){
            throw e;
        }catch (Exception e) {
            log.error("新增老人异常", e);
            throw new RuntimeException("系统繁忙，请联系管理员");
        }finally {
            //释放锁必须保证当前线程持有锁并且上锁成功
            if(isLock&&lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }

    }

    /**
     * 3. 更新老人 带延迟双删除
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateElderlyWithBed(ElderlyUpdateDTO elderlyUpdateDTO) {

        String key=RedisConstants.BED_CACHE_KEY+elderlyUpdateDTO.getBedId();
        //第一次删除redis缓存
        stringRedisTemplate.delete(key);

        // 1 查旧数据
        ElderlyEntity oldData = getById(elderlyUpdateDTO.getId());

        //2 空判断
        if (oldData == null) throw new CustomException(ResultCode.ELDER_EXIST);


        //3 床位改变
        if(!Objects.equals(oldData.getBedId(), elderlyUpdateDTO.getBedId())) {
            if(oldData.getBedId() != null) updateBedStatus(oldData.getBedId(),0);
            if(elderlyUpdateDTO.getId()!=null) updateBedStatus(elderlyUpdateDTO.getBedId(),1);
        }

        //4 更新
        BeanUtil.copyProperties(elderlyUpdateDTO,oldData);

        boolean updateSuccess= this.updateById(oldData);

        //异步延迟第二次删除缓存
        if(updateSuccess){
            // 只有在事务真正 Committed 之后，才触发
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 此时数据库已经板上钉钉是新数据了
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(1000);
                            stringRedisTemplate.delete(key);
                        } catch (Exception e) {
                            log.error("延时双删异常", e);
                        }
                    });
                }
            });
        }

        return updateSuccess;
    }

    /**
     * 4. 删除老人 (退住)
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteLogic(String ids) {
        //1 转为id list
        List<Long> idList = Func.toLongList(ids);

        // 2. 找出这些老人占用的所有床位 ID (为了批量释放)
        // 性能优化：一次性查出所有要删除的老人，避免在循环里查库 调用mp的listByIds
        List<ElderlyEntity> elderlyList = this.listByIds(idList);

        List<Integer>bedIdsRelease=elderlyList.stream()
                .map(ElderlyEntity::getBedId)
                .filter(Objects::nonNull)//排除没有床位的老人
                .distinct()//去重
                .toList();

        //批量释放床位
        if(!bedIdsRelease.isEmpty()){
            bedMapper.update(null,new LambdaUpdateWrapper<BedEntity>()
                    .in(BedEntity::getId,bedIdsRelease)
                    .set(BedEntity::getStatus,0));
        }

        //批量删除老人
        return this.removeByIds(idList);
    }

    /*
    * 5 查看单个老人 通过redis缓存
    * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ElderlyVO detail(Long id) {
        //防止穿透的redis查询缓存
        ElderlyEntity elderlyEntity = cacheClient.queryWithPassThrough(
                RedisConstants.ELDERLY_CACHE_KEY,
                id,
                ElderlyEntity.class,
                this::getById,
                30L,
                TimeUnit.MINUTES
        );

        if(elderlyEntity == null) return null;

        ElderlyVO elderlyVO = BeanUtil.copyProperties(elderlyEntity, ElderlyVO.class);

        //包装
        this.setParaStr(Collections.singletonList(elderlyVO));

        return elderlyVO;

    }


    @Override
    public IPage<ElderlyVO> listElderlyWithImagingByPage(Integer pageNum, Integer pageSize){

        IPage<ElderlyVO> page = new Page<>(pageNum, pageSize);

        return  elderlyMapper.listElderlyWithImagingByPage(page);
    }


    /*
     * 辅助方法 ：根据传入的 bedId，去更新 bed 表里的 status 字段
     * 返回 boolean 类型
     * 增加乐观锁条件 (eq(BedEntity::getStatus, 0))
     * */
    private boolean updateBedStatus(Integer bedId, Integer status) {
        // 只有当 status = 1 (占用床位) 时，才需要判断原状态是否为 0 (空闲)
        // 如果是 status = 0 (释放床位)，可能不需要这么严苛的判断
        LambdaUpdateWrapper<BedEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BedEntity::getId,bedId).set(BedEntity::getStatus,status);

        if(status==1){
            updateWrapper.eq(BedEntity::getStatus,0);//如果要修改为占用 本来必须是空闲
        }

        int rows=bedMapper.update(null,updateWrapper);

        return rows>0;

    }


    public Result<String> sign(){
        //获取当前用户
        SysUser sysUser = UserContext.getUser();
        if(sysUser==null){
            return Result.error("401","当前用户不存在");
        }
        if(!sysUser.getRole().equals("ELDERLY")){
            return Result.error("401","请用老人身份登录");
        }
        Long elderId=sysUser.getLinkId();
        if(elderId==null){
            return Result.error("401","该用户没有关联老人");
        }
        LocalDateTime now=LocalDateTime.now();

        String key=RedisConstants.ELDERLY_SIGN_KEY+elderId+now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        int offset=now.getDayOfMonth()-1;//得出偏移量
        Boolean isAlreadySigned =stringRedisTemplate.opsForValue().setBit(key, offset, true);
        if(Boolean.TRUE.equals(isAlreadySigned)){
            return Result.error("401","您今日已经打卡过了，请勿重复操作");
        }


        return Result.success("今日健康打卡成功");

    }

    public int getContinuousSignCount(){
        SysUser sysUser = UserContext.getUser();
        LocalDateTime now=LocalDateTime.now();
        Long elderId=sysUser.getLinkId();
        String key=RedisConstants.ELDERLY_SIGN_KEY+elderId+now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        int dayOfMonth=now.getDayOfMonth();

        //获取本月截止到今天的打卡记录
        List<Long> result=stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );//
        if(result==null||result.isEmpty()){
            return 0;
        }
        Long num=result.get(0);//
        if(num==null||num==0){
            return 0;
        }
        // 3. 循环位运算，计算末尾连续 1 的个数
        int count = 0;
        while (true) {
            // 让数字与 1 做与运算，判断最后一位是不是 1
            if ((num & 1) == 0) {
                // 如果最后一位是 0，说明连续中断了
                break;
            } else {
                count++;
            }
            // 右移一位，检查前一天
            num >>>= 1;
        }
        return count;

    }
    private void setParaStr(List<ElderlyVO> list){

        if(Func.isEmpty(list)){
            return;
        }

        //1查询出床位
        //1 批量查询所有的床位
        List<Integer>bedIds=list.stream()
                .map(ElderlyVO::getBedId)
                .filter(Func::isNotEmpty)
                .distinct()
                .toList();

        Map<Integer,String> bedMap;
        if(Func.isNotEmpty(bedIds)){
            //转换出Map<bedId,bedNumber>
            bedMap=bedService.listByIds(bedIds).stream()
                    .collect(Collectors.toMap(BedEntity::getId,BedEntity::getBedNumber));
        }else{
            bedMap = new HashMap<>(); // 防止空指针
        }

        //2 批量查出所有的护工
        List<Integer>employeeIds=list.stream()
                .map(ElderlyVO::getNurseId)
                .filter(Func::isNotEmpty)
                .distinct()
                .toList();

        Map<Integer,String> employeeNameMap;
        Map<Integer,String> employeePhoneMap;
        if(Func.isNotEmpty(employeeIds)){
            employeeNameMap= IEmployeeService.listByIds(employeeIds).stream()
                    .collect(Collectors.toMap(EmployeeEntity::getId, EmployeeEntity::getName));

            employeePhoneMap= IEmployeeService.listByIds(employeeIds).stream()
                    .collect(Collectors.toMap(EmployeeEntity::getId, EmployeeEntity::getPhone));
        }else{
            employeeNameMap=new HashMap<>();
            employeePhoneMap=new HashMap<>();
        }

        //3 内存组装
        list.forEach(vo -> {

            //填充字典
            vo.setGenderStr(sysDictService.getValue("gender",Func.toStr(vo.getGender())));
            vo.setHealthStatusStr(sysDictService.getValue("health_status",Func.toStr(vo.getHealthStatus())));

            //填充外键
            vo.setBedNumber(bedMap.getOrDefault(vo.getBedId(),"未分配"));
            vo.setNurseName(employeeNameMap.getOrDefault(vo.getNurseId(),"暂无护工"));
            vo.setNursePhone(employeePhoneMap.getOrDefault(vo.getNurseId(),"暂无电话"));
        });
    }

}