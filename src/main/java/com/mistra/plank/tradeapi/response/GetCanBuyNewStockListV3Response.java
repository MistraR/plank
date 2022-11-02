package com.mistra.plank.tradeapi.response;

import java.util.List;

public class GetCanBuyNewStockListV3Response extends BaseTradeResponse {

    private List<NewQuotaInfo> NewQuota;
    private List<NewStock> NewStockList;

    public List<NewQuotaInfo> getNewQuota() {
        return NewQuota;
    }

    public void setNewQuota(List<NewQuotaInfo> newQuota) {
        NewQuota = newQuota;
    }

    public List<NewStock> getNewStockList() {
        return NewStockList;
    }

    public void setNewStockList(List<NewStock> newStockList) {
        NewStockList = newStockList;
    }

    public static class NewQuotaInfo {
        private String Gddm;
        private String Kcbsged;
        private String Ksgsz;
        private String Market;

        public String getGddm() {
            return Gddm;
        }

        public void setGddm(String gddm) {
            Gddm = gddm;
        }

        public String getKcbsged() {
            return Kcbsged;
        }

        public void setKcbsged(String kcbsged) {
            Kcbsged = kcbsged;
        }

        public String getKsgsz() {
            return Ksgsz;
        }

        public void setKsgsz(String ksgsz) {
            Ksgsz = ksgsz;
        }

        public String getMarket() {
            return Market;
        }

        public void setMarket(String market) {
            Market = market;
        }
    }

    public static class NewStock {
        private String Market;
        private String Sgrq;
        private String Zqdm;
        private String Zqmc;
        private String Sgdm;
        private String Fxzs;
        private String Wsfxs;
        private String Fxj;
        private String Yc_Fxj;
        private String Sgsx;
        private String Yc_Sgsx;
        private String Sgzjsx;
        private String Yc_Sgzjsx;
        private String Ksgsx;
        private String SgState;
        private String Min_Step;
        private String CDR_Flag;
        private String YL_Flag;
        private String TPQCY_FLag;
        private String Cybbz;
        private String SFZCZ;
        private String JYXYJG;
        private String APPLYPRICE;

        public String getMarket() {
            return Market;
        }

        public void setMarket(String market) {
            Market = market;
        }

        public String getSgrq() {
            return Sgrq;
        }

        public void setSgrq(String sgrq) {
            Sgrq = sgrq;
        }

        public String getZqdm() {
            return Zqdm;
        }

        public void setZqdm(String zqdm) {
            Zqdm = zqdm;
        }

        public String getZqmc() {
            return Zqmc;
        }

        public void setZqmc(String zqmc) {
            Zqmc = zqmc;
        }

        public String getSgdm() {
            return Sgdm;
        }

        public void setSgdm(String sgdm) {
            Sgdm = sgdm;
        }

        public String getFxzs() {
            return Fxzs;
        }

        public void setFxzs(String fxzs) {
            Fxzs = fxzs;
        }

        public String getWsfxs() {
            return Wsfxs;
        }

        public void setWsfxs(String wsfxs) {
            Wsfxs = wsfxs;
        }

        public String getFxj() {
            return Fxj;
        }

        public void setFxj(String fxj) {
            Fxj = fxj;
        }

        public String getYc_Fxj() {
            return Yc_Fxj;
        }

        public void setYc_Fxj(String yc_Fxj) {
            Yc_Fxj = yc_Fxj;
        }

        public String getSgsx() {
            return Sgsx;
        }

        public void setSgsx(String sgsx) {
            Sgsx = sgsx;
        }

        public String getYc_Sgsx() {
            return Yc_Sgsx;
        }

        public void setYc_Sgsx(String yc_Sgsx) {
            Yc_Sgsx = yc_Sgsx;
        }

        public String getSgzjsx() {
            return Sgzjsx;
        }

        public void setSgzjsx(String sgzjsx) {
            Sgzjsx = sgzjsx;
        }

        public String getYc_Sgzjsx() {
            return Yc_Sgzjsx;
        }

        public void setYc_Sgzjsx(String yc_Sgzjsx) {
            Yc_Sgzjsx = yc_Sgzjsx;
        }

        public String getKsgsx() {
            return Ksgsx;
        }

        public void setKsgsx(String ksgsx) {
            Ksgsx = ksgsx;
        }

        public String getSgState() {
            return SgState;
        }

        public void setSgState(String sgState) {
            SgState = sgState;
        }

        public String getMin_Step() {
            return Min_Step;
        }

        public void setMin_Step(String min_Step) {
            Min_Step = min_Step;
        }

        public String getCDR_Flag() {
            return CDR_Flag;
        }

        public void setCDR_Flag(String cDR_Flag) {
            CDR_Flag = cDR_Flag;
        }

        public String getYL_Flag() {
            return YL_Flag;
        }

        public void setYL_Flag(String yL_Flag) {
            YL_Flag = yL_Flag;
        }

        public String getTPQCY_FLag() {
            return TPQCY_FLag;
        }

        public void setTPQCY_FLag(String tPQCY_FLag) {
            TPQCY_FLag = tPQCY_FLag;
        }

        public String getCybbz() {
            return Cybbz;
        }

        public void setCybbz(String cybbz) {
            Cybbz = cybbz;
        }

        public String getSFZCZ() {
            return SFZCZ;
        }

        public void setSFZCZ(String sFZCZ) {
            SFZCZ = sFZCZ;
        }

        public String getJYXYJG() {
            return JYXYJG;
        }

        public void setJYXYJG(String jYXYJG) {
            JYXYJG = jYXYJG;
        }

        public String getAPPLYPRICE() {
            return APPLYPRICE;
        }

        public void setAPPLYPRICE(String aPPLYPRICE) {
            APPLYPRICE = aPPLYPRICE;
        }

    }
}
