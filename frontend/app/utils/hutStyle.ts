import type { Hut } from '~/types/booking'

/**
 * One colour per hut, one shape per status.
 *
 * Chosen for red-green colour blindness: violet, amber and sky sit on the
 * blue-yellow axis, which deuteranopia and protanopia leave intact, unlike the
 * green/orange pairing this replaced. They also differ in lightness, so they
 * stay apart even in greyscale.
 *
 * Colour is never the only signal. Status is carried by fill vs dashed outline,
 * the hut owns its row, and the block prints the hut name - so a booking is
 * still readable if the hues are indistinguishable.
 *
 * Classes are written out in full because Tailwind only detects literal names.
 *
 * Approved (solid) blocks carry a darker same-hue border plus a faint inset
 * ring, so a block keeps a defined edge even when it sits right next to another
 * booking of the same hut - colour alone would let the two merge into one bar.
 */
export interface HutStyle {
  /** Approved: solid fill. */
  approved: string
  /** Requested: dashed outline over a wash of the same hue. */
  requested: string
  /** Legend swatch. */
  swatch: string
}

const NEUTRAL: HutStyle = {
  approved: 'bg-zinc-500 text-white border border-zinc-700 ring-1 ring-inset ring-white/15',
  requested: 'border-2 border-dashed border-zinc-500 bg-zinc-500/15 text-highlighted',
  swatch: 'bg-zinc-500'
}

const HUT_STYLES: Record<Hut, HutStyle> = {
  HULDREBAKKEN: {
    approved: 'bg-violet-600 text-white border border-violet-800 ring-1 ring-inset ring-white/20',
    requested: 'border-2 border-dashed border-violet-500 bg-violet-500/15 text-highlighted',
    swatch: 'bg-violet-600'
  },
  TROLLHAUGEN: {
    approved: 'bg-amber-500 text-zinc-950 border border-amber-700 ring-1 ring-inset ring-black/15',
    requested: 'border-2 border-dashed border-amber-500 bg-amber-500/15 text-highlighted',
    swatch: 'bg-amber-500'
  },
  TENT_HAMMOCK: {
    approved: 'bg-sky-400 text-zinc-950 border border-sky-600 ring-1 ring-inset ring-black/15',
    requested: 'border-2 border-dashed border-sky-400 bg-sky-400/15 text-highlighted',
    swatch: 'bg-sky-400'
  }
}

/** Falls back to neutral so a hut added backend-first still renders. */
export function hutStyle(hut: Hut | string): HutStyle {
  return HUT_STYLES[hut as Hut] ?? NEUTRAL
}

export function hutBlockClass(hut: Hut | string, approved: boolean): string {
  const style = hutStyle(hut)
  return approved ? style.approved : style.requested
}
