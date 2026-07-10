export interface Hut {
  id: number
  name: string
}

const huts: Hut[] = [
  { id: 1, name: 'Huldrebakken' },
  { id: 2, name: 'Trollhaugen' },
  { id: 3, name: 'Tent/hengekøye' }
]

export function useHuts() {
  return huts
}
