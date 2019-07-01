package seckill.demo.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;
import seckill.demo.domain.SeckillUser;

/**
 * 秒杀用户表user的SQl Mapper
 */
@Mapper
@Repository
public interface SeckillUserDao {

    /**
     * 根据id查询秒杀用户信息
     * @param id
     * @return
     */
    @Select("select * from seckill_user where id = #{id}")
    SeckillUser getById(@Param("id") Long id);

    /**
     *
     * @param updatedUser
     */
    @Update("UPDATE seckill_user SET password=#{password} WHERE id=#{id}")
    void updatePassword(SeckillUser updatedUser);
}
