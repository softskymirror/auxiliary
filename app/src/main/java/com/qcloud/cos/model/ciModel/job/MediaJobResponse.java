package com.qcloud.cos.model.ciModel.job;

import com.qcloud.cos.model.CiServiceResult;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * 媒体处理 任务响应实体 https://cloud.tencent.com/document/product/460/48234
 */
@XStreamAlias("Response")
public class MediaJobResponse extends CiServiceResult {
    @XStreamAlias("JobsDetail")
    private MediaJobObject jobsDetail;

    public MediaJobObject getJobsDetail() {
        if (jobsDetail==null){
            jobsDetail = new MediaJobObject();
        }
        return jobsDetail;
    }

    @Override
    public String toString() {
        return "MediaJobResponse{" +
                "jobsDetail=" + jobsDetail +
                '}';
    }
}
