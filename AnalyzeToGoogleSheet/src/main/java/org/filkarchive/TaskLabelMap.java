package org.filkarchive;

import java.util.*;

class TaskLabelMap
{
    final NavigableMap<String, String> taskLabels = new TreeMap<>();
    final Map<String, Double> taskLabelWorkflowVersions = new HashMap<>();

    public void updateTaskLabels(
        ZooniverseClassificationEntry classification)
    {
        Double workflowVersion = Double
            .parseDouble(classification.fields.get("workflow_version"));

        for (Map<String, Object> annotation : classification.annotations)
        {
            String taskId = (String) annotation.get("task");
            String taskLabel = (String) annotation.get("task_label");

            trackTaskLabel(taskId, taskLabel, workflowVersion);
        }
    }

    public void trackTaskLabel(String taskId, String taskLabel, Double workflowVersion)
    {
        if (!taskLabelWorkflowVersions.containsKey(taskId) ||
            taskLabelWorkflowVersions.get(taskId) < workflowVersion) {
            taskLabels.put(taskId, taskLabel);
            taskLabelWorkflowVersions.put(taskId, workflowVersion);
        }
    }

    public List<String> getTaskIds()
    {
        return new ArrayList<>(taskLabels.navigableKeySet());
    }

    void copy(TaskLabelMap other)
    {
        taskLabels.putAll(other.taskLabels);
        taskLabelWorkflowVersions.putAll(other.taskLabelWorkflowVersions);
    }
}
