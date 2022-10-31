package com.mistra.plank.mapper;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.pojo.model.po.DailyIndex;
import com.mistra.plank.pojo.model.vo.DailyIndexVo;
import com.mistra.plank.pojo.model.vo.PageParam;
import com.mistra.plank.pojo.model.vo.PageVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailyIndexDao extends BaseMapper<DailyIndex> {

    void save(List<DailyIndex> list);

    PageVo<DailyIndexVo> getDailyIndexList(PageParam pageParam);

    List<DailyIndex> getDailyIndexListByDate(Date date);

}
