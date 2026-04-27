package com.uet.BiddingApplication.Config;

public class Main {
    public static void main(String[] args) {
        System.out.println(AppConfig.getServerPort());
        System.out.println(AppConfig.getWorkerPoolSize());
    }
}
