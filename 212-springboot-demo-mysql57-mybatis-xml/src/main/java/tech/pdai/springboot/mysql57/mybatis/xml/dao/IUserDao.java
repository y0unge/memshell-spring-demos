package tech.pdai.springboot.mysql57.mybatis.xml.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import tech.pdai.springboot.mysql57.mybatis.xml.entity.User;
import tech.pdai.springboot.mysql57.mybatis.xml.entity.query.UserQueryBean;

/**
 * @author pdai
 */
@Mapper
public interface IUserDao {

    List<User> findList(UserQueryBean userQueryBean);

    User findById(Long id);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);

    int update(User user);

    int save(User user);

    int updatePassword(User user);
}
