package com.mistra.plank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.pojo.entity.DailyIndex;
import com.mistra.plank.pojo.vo.DailyIndexVo;
import com.mistra.plank.pojo.vo.PageParam;
import com.mistra.plank.pojo.vo.PageVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

@Mapper
public interface DailyIndexDao extends BaseMapper<DailyIndex> {

    void save(List<DailyIndex> list);

    PageVo<DailyIndexVo> getDailyIndexList(PageParam pageParam);

    List<DailyIndex> getDailyIndexListByDate(Date date);

}
