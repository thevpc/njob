package net.thevpc.nuts.toolbox.njob.model;

import net.thevpc.nuts.toolbox.njob.time.TimePeriod;

import java.time.Instant;
import java.util.Objects;

public class NTask {
    @Id
    private String id;
    private String jobId;
    private String parentTaskId;
    private String name;
    private NPriority priority;
    private NFlag flag;
    private Instant creationTime;
    private Instant modificationTime;
    private Instant dueTime;
    private Instant startTime;
    private Instant endTime;
    private TimePeriod internalDuration;
    private TimePeriod duration;
    private String project;

    private NTaskStatus status = NTaskStatus.TODO;

    private String observations;

    public String getName() {
        return name;
    }

    public NTask setName(String name) {
        this.name = name;
        return this;
    }

    public String getProject() {
        return project;
    }

    public NTask setProject(String project) {
        this.project = project;
        return this;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public NTask setStartTime(Instant startTime) {
        this.startTime = startTime;
        return this;
    }

    public TimePeriod getInternalDuration() {
        return internalDuration;
    }

    public NTask setInternalDuration(TimePeriod internalDuration) {
        this.internalDuration = internalDuration;
        return this;
    }

    public TimePeriod getDuration() {
        return duration;
    }

    public NTask setDuration(TimePeriod duration) {
        this.duration = duration;
        return this;
    }

    public String getId() {
        return id;
    }

    public NTask setId(String id) {
        this.id = id;
        return this;
    }

    public String getObservations() {
        return observations;
    }

    public NTask setObservations(String observations) {
        this.observations = observations;
        return this;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public NTask setEndTime(Instant endTime) {
        this.endTime = endTime;
        return this;
    }

    public NTaskStatus getStatus() {
        return status;
    }

    public NTask setStatus(NTaskStatus status) {
        this.status = status;
        return this;
    }

    public NFlag getFlag() {
        return flag;
    }

    public NTask setFlag(NFlag flag) {
        this.flag = flag;
        return this;
    }

    public String getJobId() {
        return jobId;
    }

    public NTask setJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    public String getParentTaskId() {
        return parentTaskId;
    }

    public NTask setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
        return this;
    }

    public Instant getDueTime() {
        return dueTime;
    }

    public NTask setDueTime(Instant dueTime) {
        this.dueTime = dueTime;
        return this;
    }

    public NPriority getPriority() {
        return priority;
    }

    public NTask setPriority(NPriority priority) {
        this.priority = priority;
        return this;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public NTask setCreationTime(Instant creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    public Instant getModificationTime() {
        return modificationTime;
    }

    public NTask setModificationTime(Instant modificationTime) {
        this.modificationTime = modificationTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NTask nTask = (NTask) o;
        return Objects.equals(id, nTask.id) && Objects.equals(jobId, nTask.jobId) && Objects.equals(parentTaskId, nTask.parentTaskId) && Objects.equals(name, nTask.name) && priority == nTask.priority && flag == nTask.flag && Objects.equals(creationTime, nTask.creationTime) && Objects.equals(modificationTime, nTask.modificationTime) && Objects.equals(dueTime, nTask.dueTime) && Objects.equals(startTime, nTask.startTime) && Objects.equals(endTime, nTask.endTime) && Objects.equals(internalDuration, nTask.internalDuration) && Objects.equals(duration, nTask.duration) && Objects.equals(project, nTask.project) && status == nTask.status && Objects.equals(observations, nTask.observations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, jobId, parentTaskId, name, priority, flag, creationTime, modificationTime, dueTime, startTime, endTime, internalDuration, duration, project, status, observations);
    }
}
