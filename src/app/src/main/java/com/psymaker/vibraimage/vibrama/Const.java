package com.psymaker.vibraimage.vibrama;

/**
 * Created by user on 31.05.2017.
 */

public final class Const {
    public final static String ARG_POSITION = "position";

    public final static int VI_PRESET_VI		= 0x0001;
    public final static int VI_PRESET_AV		= 0x0002;
    public final static int VI_PRESET_AR		= 0x0003;
    public final static int VI_PRESET_LD		= 0x0004;

    public final static int VI_RESULT_VI0_A		= 0x0002;
    public final static int VI_RESULT_VI1_A		= 0x0004;
    public final static int VI_RESULT_VI2_A		= 0x0008;
    public final static int VI_RESULT_DELTA_A	= 0x0010;
    public final static int VI_RESULT_DELTA_FA	= 0x0020;

    public final static int VI_RESULT_VI0_B		= 0x0200;
    public final static int VI_RESULT_VI1_B		= 0x0400;
    public final static int VI_RESULT_VI2_B		= 0x0800;
    public final static int VI_RESULT_DELTA_B	= 0x1000;
    public final static int VI_RESULT_DELTA_FB	= 0x2000;

    public final static int VI_RESULT_SRC_0		= 0x8000;

    public final static int VT_NULL=0;
    public final static int VT_INT=1;
    public final static int VT_FLOAT=2;
    public final static int VT_STR=3;
}
