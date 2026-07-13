"use client";

import { Avatar } from "antd";
import type { AvatarProps } from "antd";
import { useQuery } from "@tanstack/react-query";
import { UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import { fetchProtectedFileBlob } from "@/lib/protected-files";

type UserAvatarProps = Omit<AvatarProps, "src"> & {
  avatarUrl?: string;
  iconSize?: number;
};

export function UserAvatar({ avatarUrl, iconSize = 18, ...props }: UserAvatarProps) {
  const avatar = useQuery({
    queryKey: ["user-avatar", avatarUrl],
    queryFn: () => fetchProtectedFileBlob(avatarUrl!),
    enabled: Boolean(avatarUrl),
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
  const [objectUrl, setObjectUrl] = useState<string>();

  useEffect(() => {
    if (!avatar.data) {
      setObjectUrl(undefined);
      return;
    }
    const nextUrl = URL.createObjectURL(avatar.data);
    setObjectUrl(nextUrl);
    return () => URL.revokeObjectURL(nextUrl);
  }, [avatar.data]);

  return <Avatar {...props} src={objectUrl} icon={<UserRound size={iconSize} />} />;
}
