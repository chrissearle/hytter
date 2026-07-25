export interface VersionInfo {
  version: string
}

export const VERSION_CACHE_KEY = 'version'

export function useVersion() {
  return useFetch<VersionInfo>('/version', { key: VERSION_CACHE_KEY })
}
