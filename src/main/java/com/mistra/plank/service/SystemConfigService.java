package com.mistra.plank.service;


import com.mistra.plank.model.entity.SystemConfig;

import java.util.List;

public interface SystemConfigService {

    boolean isMock();

    boolean isApplyNewConvertibleBond();

    boolean isCr();

    List<SystemConfig> getAll();

}
