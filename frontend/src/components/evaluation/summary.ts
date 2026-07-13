import type { EvaluationApplication, EvaluationSummary, EvaluationTaskOption } from "./types";

export function buildEvaluationSummary(tasks: EvaluationTaskOption[], applications: EvaluationApplication[], today = "2024-05-20"): EvaluationSummary {
  const todayTime = new Date(today).getTime();
  const ongoingTasks = tasks.filter((task) => task.status === "进行中");
  const nearDeadlineTaskCount = ongoingTasks.filter((task) => {
    const days = (new Date(task.dueDate).getTime() - todayTime) / 86400000;
    return days >= 0 && days <= 30;
  }).length;
  const submittedTimes = applications
    .filter((application) => application.status === "pending" || application.status === "reviewing")
    .map((application) => application.submittedAt ? todayTime - new Date(application.submittedAt).getTime() : 0);
  const longestWaitingDays = submittedTimes.length > 0 ? Math.max(0, Math.ceil(Math.max(...submittedTimes) / 86400000)) : 0;
  return {
    taskTotal: tasks.length,
    taskNewCount: Math.min(4, tasks.length),
    ongoingTaskCount: ongoingTasks.length,
    nearDeadlineTaskCount,
    applicationTotal: applications.length,
    draftCount: applications.filter((application) => application.status === "draft").length,
    pendingCount: applications.filter((application) => application.status === "pending" || application.status === "reviewing").length,
    longestWaitingDays,
  };
}
