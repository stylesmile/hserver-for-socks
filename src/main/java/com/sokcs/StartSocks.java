package com.sokcs;

import top.hserver.HServerApplication;

/**
 * @author hxm
 */
public class StartSocks {
    public static void main(String[] args) {
        HServerApplication.run(StartSocks.class, 12120, args);
    }

}
