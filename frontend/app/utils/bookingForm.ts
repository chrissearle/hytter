export const NAME_MAX_LENGTH = 100

/** Mirrors ADMIN_NOTES_MAX_LENGTH in BookingAdminRoutes.kt. */
export const ADMIN_NOTES_MAX_LENGTH = 2000

export interface BookingFormFieldError {
  name: string
  message: string
}

export interface BookingFormState {
  /** The free-text name. Only validated when [requiresName] is set. */
  name: string
  /** True when the selected name type shows a free-text field - see needsNameInput. */
  requiresName: boolean
  numberOfPeople: number | undefined
  arrivalDate: string
  departureDate: string
}

export function validateBookingForm(state: BookingFormState): BookingFormFieldError[] {
  const errors: BookingFormFieldError[] = []
  const trimmedName = state.name.trim()

  if (state.requiresName) {
    if (!trimmedName) {
      errors.push({ name: 'name', message: 'Navn er påkrevd' })
    } else if (trimmedName.length > NAME_MAX_LENGTH) {
      errors.push({ name: 'name', message: `Navn kan være maks ${NAME_MAX_LENGTH} tegn` })
    }
  }

  if (!state.numberOfPeople || state.numberOfPeople < 1) {
    errors.push({ name: 'numberOfPeople', message: 'Antall personer må være minst 1' })
  }

  if (!state.arrivalDate) {
    errors.push({ name: 'arrivalDate', message: 'Ankomstdato er påkrevd' })
  }

  if (!state.departureDate) {
    errors.push({ name: 'departureDate', message: 'Avreisedato er påkrevd' })
  } else if (state.arrivalDate && state.departureDate < state.arrivalDate) {
    errors.push({ name: 'departureDate', message: 'Avreise kan ikke være før ankomst' })
  }

  return errors
}
