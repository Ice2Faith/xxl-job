package com.xxl.job.admin.business.model.dto;

import com.xxl.job.admin.business.model.XxlJobLog;
import com.xxl.tool.core.DateTool;
import lombok.Data;

@Data
public class XxlJobLogDTO {

    private long id;

    // job info
    private int jobGroup;
    private int jobId;

    // execute info
    private String executorAddress;
    private String executorHandler;
    private String executorParam;
    private String executorShardingParam;
    private int executorFailRetryCount;

    // trigger info
    private String triggerTime;
    private int triggerCode;
    private String triggerMsg;

    // handle info
    private String handleTime;
    private int handleCode;
    private String handleMsg;

    // alarm info
    private int alarmStatus;

    // more info
    protected String jobName;
    protected String groupName;

    public XxlJobLogDTO(XxlJobLog xxlJobLog) {
        this.id = xxlJobLog.getId();
        this.jobGroup = xxlJobLog.getJobGroup();
        this.jobId = xxlJobLog.getJobId();
        this.executorAddress = xxlJobLog.getExecutorAddress();
        this.executorHandler = xxlJobLog.getExecutorHandler();
        this.executorParam = xxlJobLog.getExecutorParam();
        this.executorShardingParam = xxlJobLog.getExecutorShardingParam();
        this.executorFailRetryCount = xxlJobLog.getExecutorFailRetryCount();
        this.triggerTime = xxlJobLog.getTriggerTime() != null ? DateTool.formatDateTime(xxlJobLog.getTriggerTime()) : null;
        this.triggerCode = xxlJobLog.getTriggerCode();
        this.triggerMsg = xxlJobLog.getTriggerMsg();
        this.handleTime = xxlJobLog.getHandleTime() != null ? DateTool.formatDateTime(xxlJobLog.getHandleTime()) : null;
        this.handleCode = xxlJobLog.getHandleCode();
        this.handleMsg = xxlJobLog.getHandleMsg();
        this.alarmStatus = xxlJobLog.getAlarmStatus();

        this.jobName=xxlJobLog.getJobName();
        this.groupName=xxlJobLog.getGroupName();
    }


}
