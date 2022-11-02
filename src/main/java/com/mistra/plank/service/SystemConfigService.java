package com.mistra.plank.service;


import com.mistra.plank.pojo.entity.SystemConfig;

import java.util.List;

public interface SystemConfigService {

    boolean isMock();

    boolean isApplyNewConvertibleBond();

    boolean isCr();

    List<SystemConfig> getAll();

}
