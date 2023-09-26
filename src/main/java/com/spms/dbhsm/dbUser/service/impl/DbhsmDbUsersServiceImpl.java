package com.spms.dbhsm.dbUser.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.constant.DbConstants;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.dbUser.mapper.DbhsmDbUsersMapper;
import com.spms.dbhsm.dbUser.mapper.DbhsmUserDbInstanceMapper;
import com.spms.dbhsm.dbUser.service.IDbhsmDbUsersService;
import com.spms.dbhsm.permissionGroup.domain.DbhsmPermissionGroup;
import com.spms.dbhsm.permissionGroup.domain.dto.PermissionGroupForUserDto;
import com.spms.dbhsm.permissionGroup.mapper.DbhsmPermissionGroupMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 数据库用户Service业务层处理
 *
 * @author ccsp
 * @date 2023-09-25
 */
@Service
@Slf4j
public class DbhsmDbUsersServiceImpl implements IDbhsmDbUsersService {
    @Autowired
    private DbhsmDbUsersMapper dbhsmDbUsersMapper;

    @Autowired
    private DbhsmDbInstanceMapper dbhsmDbInstanceMapper;

    @Autowired
    private DbhsmPermissionGroupMapper dbhsmPermissionGroupMapper;

    @Autowired
    DbhsmUserDbInstanceMapper  dbhsmUserDbInstanceMapper;
    /**
     * 查询数据库用户
     *
     * @param id 数据库用户主键
     * @return 数据库用户
     */
    @Override
    public DbhsmDbUser selectDbhsmDbUsersById(Long id) {
        return dbhsmDbUsersMapper.selectDbhsmDbUsersById(id);
    }

    /**
     * 查询数据库用户列表
     *
     * @param dbhsmDbUser 数据库用户
     * @return 数据库用户
     */
    @Override
    public List<DbhsmDbUser> selectDbhsmDbUsersList(DbhsmDbUser dbhsmDbUser) {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<DbhsmDbUser> dbhsmDbUsersResult = new ArrayList<DbhsmDbUser>();
        Connection conn = null;
        //查询所有的数据库实例
        DbhsmDbInstance dbhsmDbInstance = new DbhsmDbInstance();
        List<DbhsmDbInstance> instancesList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(dbhsmDbInstance);
        //查询通过web界面创建的所有用户,用于区分哪些属于通过web创建的用户
        List<DbhsmDbUser> dbhsmDbUsers = dbhsmDbUsersMapper.selectDbhsmDbUsersList(dbhsmDbUser);
        //根据用户与数据库实例的对应关系表，把实例DbInstanceGetConnDTO赋值给用户。
        dbhsmDbUsers.forEach(dbUser -> {
            DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceByUserId(dbUser.getId());
            if (instance != null) {
                DbInstanceGetConnDTO getConnDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, getConnDTO);
                dbUser.setDbInstanceGetConnDTO(getConnDTO);
            }
        });
        //遍历数据库实例，查看每个实例下的用户
        for (DbhsmDbInstance instance : instancesList) {
            try {
                //创建数据库连接
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance,connDTO);
                conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                if (Optional.ofNullable(conn).isPresent()) {
                    //查询数据库实例用户列表
                    String sql = "select  * from all_users where INHERITED='NO'";
                    preparedStatement = conn.prepareStatement(sql);
                    resultSet = preparedStatement.executeQuery();
                    //对返回结果进行封装用户对象
                    while (resultSet.next()) {
                        DbhsmDbUser dbUser = new DbhsmDbUser();
                        dbUser.setUserName(resultSet.getString("userName"));
                        dbUser.setUserId(resultSet.getString("user_id"));
                        dbUser.setCreated(resultSet.getDate("created"));
                        dbUser.setCommon(resultSet.getString("common"));
                        dbUser.setAllShard(resultSet.getString("all_shard"));
                        dbUser.setDefaultCollation(resultSet.getString("default_collation"));
                        dbUser.setInherited(resultSet.getString("inherited"));
                        dbUser.setOracleMaintained(resultSet.getString("oracle_maintained"));
                        dbUser.setImplicit(resultSet.getString("implicit"));
                        dbUser.setSecretService(instance.getSecretService());
                        dbUser.setPermissionGroupForUserDto(new PermissionGroupForUserDto());
                        connDTO.setDatabaseDbaPassword(null);
                        dbUser.setDbInstanceGetConnDTO(connDTO);
                        dbUser.setIsSelfBuilt(DbConstants.IS_NOT_SELF_BUILT);
                        //遍历数据库中的用户，如果和查询出的用户名一致说明为web端创建，设置其is_self_built属性值为0
                        dbhsmDbUsers.stream()
                                .filter(user -> dbUser.getUserName().equals(user.getUserName().toUpperCase()))
                                .findFirst()
                                .ifPresent(user -> {
                                    dbUser.setIsSelfBuilt(DbConstants.IS_SELF_BUILT);
                                    DbhsmPermissionGroup permissionGroup = dbhsmPermissionGroupMapper.selectDbhsmPermissionGroupByPermissionGroupId(user.getPermissionGroupId());
                                    PermissionGroupForUserDto permissionGroupForUser = new PermissionGroupForUserDto();
                                    BeanUtils.copyProperties(permissionGroup,permissionGroupForUser);
                                    dbUser.setPermissionGroupForUserDto(permissionGroupForUser);
                                    dbUser.setId(user.getId());
                                });
                        dbhsmDbUsersResult.add(dbUser);
                    }
                }
            } catch (ZAYKException | SQLException e) {
                e.printStackTrace();
                log.info("获取数据库连接失败！" + e.getMessage());
            }
            //释放资源
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return dbhsmDbUsersResult;
    }

    /**
     * 新增数据库用户
     *
     * @param dbhsmDbUser 数据库用户
     * @return 结果
     */
    @Override
    public int insertDbhsmDbUsers(DbhsmDbUser dbhsmDbUser) {
        return dbhsmDbUsersMapper.insertDbhsmDbUsers(dbhsmDbUser);
    }

    /**
     * 修改数据库用户
     *
     * @param dbhsmDbUser 数据库用户
     * @return 结果
     */
    @Override
    public int updateDbhsmDbUsers(DbhsmDbUser dbhsmDbUser) {
        return dbhsmDbUsersMapper.updateDbhsmDbUsers(dbhsmDbUser);
    }

    /**
     * 批量删除数据库用户
     *
     * @param ids 需要删除的数据库用户主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmDbUsersByIds(Long[] ids) {
        return dbhsmDbUsersMapper.deleteDbhsmDbUsersByIds(ids);
    }

    /**
     * 删除数据库用户信息
     *
     * @param id 数据库用户主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmDbUsersById(Long id) {
        return dbhsmDbUsersMapper.deleteDbhsmDbUsersById(id);
    }
}
