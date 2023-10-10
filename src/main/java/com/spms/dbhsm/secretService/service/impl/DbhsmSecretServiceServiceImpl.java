package com.spms.dbhsm.secretService.service.impl;

import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.StringUtils;
import com.spms.common.CommandUtil;
import com.spms.common.Int4jUtil;
import com.spms.common.constant.DbConstants;
import com.spms.common.kmip.KmipServicePoolFactory;
import com.spms.dbhsm.secretService.domain.DbhsmSecretService;
import com.spms.dbhsm.secretService.mapper.DbhsmSecretServiceMapper;
import com.spms.dbhsm.secretService.service.IDbhsmSecretServiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

/**
 * 密码服务Service业务层处理
 *
 * @author diq
 * @date 2023-09-25
 */
@Slf4j
@Service
public class DbhsmSecretServiceServiceImpl implements IDbhsmSecretServiceService {
    @Autowired
    private DbhsmSecretServiceMapper dbhsmSecretServiceMapper;

    @PostConstruct
    private void initSecretService() {
        try {
            List<DbhsmSecretService> dbhsmSecretServices = dbhsmSecretServiceMapper.selectDbhsmSecretServiceList(new DbhsmSecretService());
            if (StringUtils.isEmpty(dbhsmSecretServices)) {
                log.info("KMIP服务不存在");
                return;
            }
            for (DbhsmSecretService service:dbhsmSecretServices){
                try {
                    KmipServicePoolFactory.bind(service);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            log.info("KMIP服务加载完成");
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * 查询密码服务
     *
     * @param id 密码服务主键
     * @return 密码服务
     */
    @Override
    public DbhsmSecretService selectDbhsmSecretServiceById(Long id) {
        return dbhsmSecretServiceMapper.selectDbhsmSecretServiceById(id);
    }

    /**
     * 查询密码服务列表
     *
     * @param dbhsmSecretService 密码服务
     * @return 密码服务
     */
    @Override
    public List<DbhsmSecretService> selectDbhsmSecretServiceList(DbhsmSecretService dbhsmSecretService) {
        return dbhsmSecretServiceMapper.selectDbhsmSecretServiceList(dbhsmSecretService);
    }

    /**
     * 新增密码服务
     *
     * @param dbhsmSecretService 密码服务
     * @return 结果
     */
    @Override
    public int insertDbhsmSecretService(DbhsmSecretService dbhsmSecretService) throws Exception {
        dbhsmSecretService.setCreateTime(DateUtils.getNowDate());

        //将KMIP配置文件模板拷贝到初始化文件
        String commandCopy = "cp -f /opt/config_file/kmc.ini /opt/config_file/jsonfile/" + dbhsmSecretService.getSecretService() + ".ini";
        log.info("commandCopy  === " + commandCopy);
        int copy = Runtime.getRuntime().exec(commandCopy).waitFor();
        log.info("copy result  === " + copy);
        if (0 != copy) {
            throw new Exception("初始化密码服务配置文件失败，错误码为：" + copy + "，请检查目录/opt/config_file下是否存在kmc.ini文件");
        }
        //修改初始化文件IP port userName password
        try {
            modifySecretServiceIniFile(dbhsmSecretService);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("修改密码服务配置文件失败");
        }
        //校验密钥索引是否已经生成过密钥，如果已经生成过则不需要再生成
        /*if (DbConstants.DBHSM_GLOBLE_UNIQUE.equals(checkSecretKeyIndexUnique(dbhsmSecretService))) {
            //生成SM2密钥
            try {
                generatorSM2KeyPair(dbhsmSecretService);
            } catch (Exception e) {
                throw new Exception("生成密钥失败！"+e.getMessage());
            }
        }*/
        //创建连接，
        KmipServicePoolFactory.bind(dbhsmSecretService);
        return dbhsmSecretServiceMapper.insertDbhsmSecretService(dbhsmSecretService);
    }


    private void modifySecretServiceIniFile(DbhsmSecretService dbhsmSecretService) throws IOException {
        String fileName = dbhsmSecretService.getSecretService();
        if (StringUtils.isNotEmpty(dbhsmSecretService.getServiceIp())) {
            Int4jUtil.setValue(DbConstants.KMIP_INI_PATH + "/" + fileName + ".ini", DbConstants.KMC1, "ip", dbhsmSecretService.getServiceIp());
        }

        String kmip_port = "";
        if (StringUtils.isNotEmpty(dbhsmSecretService.getServicePort())) {
            kmip_port = dbhsmSecretService.getServicePort();
        } else {
            kmip_port = Int4jUtil.getValue(DbConstants.KMIP_INI_PATH + "/" + fileName + ".ini", DbConstants.KMC1, "KMIP_Port");
        }

        if (StringUtils.isNotEmpty(dbhsmSecretService.getServiceUrl())) {
            if (!dbhsmSecretService.getServiceUrl().startsWith("/")) {
                kmip_port += "/";
            }
            kmip_port += dbhsmSecretService.getServiceUrl();
        } else {
            kmip_port += "/v2/kmip/KMIPRequest";
        }

        Int4jUtil.setValue(DbConstants.KMIP_INI_PATH + "/" + fileName + ".ini", DbConstants.KMC1, "KMIP_Port", kmip_port);

        if (StringUtils.isNotEmpty(dbhsmSecretService.getUserName())) {
            Int4jUtil.setValue(DbConstants.KMIP_INI_PATH + "/" + fileName + ".ini", DbConstants.AUTHENTICATION, "userName", dbhsmSecretService.getUserName());
        }

        if (StringUtils.isNotEmpty(dbhsmSecretService.getPassword())) {
            Int4jUtil.setValue(DbConstants.KMIP_INI_PATH + "/" + fileName + ".ini", DbConstants.AUTHENTICATION, "password", dbhsmSecretService.getPassword());
        }
    }

    /**
     * 修改密码服务
     *
     * @param dbhsmSecretService 密码服务
     * @return 结果
     */
    @Override
    public int updateDbhsmSecretService(DbhsmSecretService dbhsmSecretService) throws IOException {
        DbhsmSecretService dbhsmSecretService1 = dbhsmSecretServiceMapper.selectDbhsmSecretServiceById(dbhsmSecretService.getId());
        if (dbhsmSecretService1.getSecretService().equals(dbhsmSecretService.getSecretService())
                && dbhsmSecretService1.getPassword().equals(dbhsmSecretService.getPassword())
                && dbhsmSecretService1.getUserName().equals(dbhsmSecretService.getUserName())
                && dbhsmSecretService1.getServiceIp().equals(dbhsmSecretService.getServiceIp())
                && dbhsmSecretService1.getServicePort().equals(dbhsmSecretService.getServicePort())) {
            return 1;
        }

        dbhsmSecretService.setUpdateTime(DateUtils.getNowDate());
        int ret = dbhsmSecretServiceMapper.updateDbhsmSecretService(dbhsmSecretService);
        modifySecretServiceIniFile(dbhsmSecretService);
        KmipServicePoolFactory.rebind(dbhsmSecretService1, dbhsmSecretService);
        return ret;
    }

    /**
     * 批量删除密码服务
     *
     * @param ids 需要删除的密码服务主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmSecretServiceByIds(Long[] ids) {
        for (Long id : ids) {
            DbhsmSecretService dbhsmSecretService = dbhsmSecretServiceMapper.selectDbhsmSecretServiceById(id);
            if (dbhsmSecretService == null) {
                continue;
            }

            KmipServicePoolFactory.unbind(dbhsmSecretService);
            String commandCopy = "rm -rf /opt/config_file/jsonfile/" + dbhsmSecretService.getSecretService() + ".ini";
            log.info("commandCopy  === " + commandCopy);
            String status = CommandUtil.exeCmd(commandCopy, 5);
            log.info("commandCopy  === " + status);
        }

        return dbhsmSecretServiceMapper.deleteDbhsmSecretServiceByIds(ids);
    }

    /**
     * 删除密码服务信息
     *
     * @param id 密码服务主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmSecretServiceById(Long id) {
        DbhsmSecretService dbhsmSecretService = dbhsmSecretServiceMapper.selectDbhsmSecretServiceById(id);
        if (dbhsmSecretService == null) {
            return 0;
        }

        KmipServicePoolFactory.unbind(dbhsmSecretService);
        String commandCopy = "rm -rf /opt/config_file/jsonfile/" + dbhsmSecretService.getSecretService() + ".ini";
        log.info("commandCopy  === " + commandCopy);
        String status = CommandUtil.exeCmd(commandCopy, 5);
        log.info("commandCopy  === " + status);
        return dbhsmSecretServiceMapper.deleteDbhsmSecretServiceById(id);
    }
}