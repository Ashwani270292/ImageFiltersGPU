package com.ashwani.filteractivity;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;

/**
 * Created by hp on 7/6/2017.
 */

public class Filters {
    private String name;
    private GPUImageFilter gpuImageFilter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GPUImageFilter getGpuImageFilter() {
        return gpuImageFilter;
    }

    public void setGpuImageFilter(GPUImageFilter gpuImageFilter) {
        this.gpuImageFilter = gpuImageFilter;
    }
}
