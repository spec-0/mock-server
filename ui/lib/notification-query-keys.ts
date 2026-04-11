export const notificationQueryKeys = {
  all: ["notifications"] as const,
  unread: (limit: number) => [...notificationQueryKeys.all, "unread", limit] as const,
  inbox: () => [...notificationQueryKeys.all, "inbox"] as const,
  pendingApprovals: (orgId: string) => ["pendingSubscriptionApprovals", orgId] as const,
};
