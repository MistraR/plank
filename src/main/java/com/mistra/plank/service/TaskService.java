package com.mistra.plank.service;


import com.mistra.plank.pojo.entity.ExecuteInfo;
import com.mistra.plank.pojo.vo.PageParam;
import com.mistra.plank.pojo.vo.PageVo;
import com.mistra.plank.pojo.vo.TaskVo;

import java.util.List;

public interface TaskService {

    List<ExecuteInfo> getTaskListById(int... id);

    List<ExecuteInfo> getPendingTaskListById(int... id);

    void executeTask(ExecuteInfo executeInfo);

    PageVo<TaskVo> getAllTask(PageParam pageParam);

    void changeTaskState(int state, int id);

}
