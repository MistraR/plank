package com.mistra.plank.service;


import com.mistra.plank.model.entity.Robot;

public interface RobotService {

    Robot getSystem();

    Robot getById(int id);

}
