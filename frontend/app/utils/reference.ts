import type { BookingNameType, Hut, NameTypeItem, Reference, Session } from '~/types/booking'

/**
 * Falls back to the raw enum value rather than an empty string: if reference
 * data is still loading or a new enum value has shipped in the backend ahead of
 * the frontend, a recognizable value beats a blank cell.
 */
export function hutDisplayName(reference: Reference | null | undefined, hut: Hut): string {
  return reference?.huts.find((item) => item.value === hut)?.displayName ?? hut
}

/**
 * The name types a given session may pick, mirroring the backend rule in
 * `BookingValidation.ensureGroupAllowed`. Admins and anonymous visitors get the
 * full list; a logged-in non-admin gets only Personlig and their own group -
 * no other group and not Annet. [keep] is included regardless, so editing a
 * booking whose type is otherwise off-limits (e.g. an admin-created one) still
 * shows its current value instead of silently dropping it.
 *
 * Purely a UX filter: the backend enforces the same rule on every write.
 */
export function allowedNameTypes(
  reference: Reference | null | undefined,
  session: Session | null | undefined,
  keep?: BookingNameType
): NameTypeItem[] {
  const all = reference?.nameTypes ?? []
  if (!session?.authenticated || session.isAdmin) return all
  return all.filter(
    (item) => item.value === 'PERSONAL' || item.value === session.group || item.value === keep
  )
}

export function nameTypeItem(
  reference: Reference | null | undefined,
  nameType: BookingNameType
): NameTypeItem | undefined {
  return reference?.nameTypes.find((item) => item.value === nameType)
}

/**
 * A free-text name field is shown for the free-text types. The exception is a
 * personal booking by a logged-in user, where the name comes from their token -
 * unless they are an admin, who takes bookings on behalf of people who ring up
 * rather than use the site and so must be able to name someone else.
 */
export function needsNameInput(
  reference: Reference | null | undefined,
  nameType: BookingNameType,
  authenticated: boolean,
  isAdmin = false
): boolean {
  if (!nameTypeItem(reference, nameType)?.isFreeText) return false
  if (nameType !== 'PERSONAL') return true
  return !authenticated || isAdmin
}
