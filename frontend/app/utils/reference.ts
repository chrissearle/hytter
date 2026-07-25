import type { BookingNameType, Hut, NameTypeItem, Reference } from '~/types/booking'

/**
 * Falls back to the raw enum value rather than an empty string: if reference
 * data is still loading or a new enum value has shipped in the backend ahead of
 * the frontend, a recognizable value beats a blank cell.
 */
export function hutDisplayName(reference: Reference | null | undefined, hut: Hut): string {
  return reference?.huts.find((item) => item.value === hut)?.displayName ?? hut
}

export function nameTypeItem(
  reference: Reference | null | undefined,
  nameType: BookingNameType
): NameTypeItem | undefined {
  return reference?.nameTypes.find((item) => item.value === nameType)
}

/**
 * A free-text name field is shown for the free-text types, except for a
 * personal booking by a logged-in user - there the name comes from their token.
 */
export function needsNameInput(
  reference: Reference | null | undefined,
  nameType: BookingNameType,
  authenticated: boolean
): boolean {
  if (!nameTypeItem(reference, nameType)?.isFreeText) return false
  return !(nameType === 'PERSONAL' && authenticated)
}
