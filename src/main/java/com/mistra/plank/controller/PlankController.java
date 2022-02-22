package com.mistra.plank.controller;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/2/22
 */

import com.mistra.plank.job.Barbarossa;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlankController {

    private final Barbarossa barbarossa;

    public PlankController(Barbarossa barbarossa) {
        this.barbarossa = barbarossa;
    }

    @PostMapping("/collectData")
    public void collectData() throws Exception {
        barbarossa.collectData();
    }

    @PostMapping("/replenish")
    public void replenish() throws Exception {
        barbarossa.replenish();
    }

    @PostMapping("/monitor")
    public void monitor(String haveStock) throws Exception {
        barbarossa.monitor(haveStock);
    }

    @PostMapping("/analyze")
    public void analyze() throws Exception {
        barbarossa.analyze();
    }

    @PostMapping("/continuousInflow")
    public void continuousInflow() throws Exception {
        barbarossa.continuousInflow();
    }
}
