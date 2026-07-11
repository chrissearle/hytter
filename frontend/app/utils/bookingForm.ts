export const NAME_MAX_LENGTH = 100
export const NAME_PRESETS = ['Opphavet', 'Sørkisrampen', 'HA12', 'Personlig', 'Annet'] as const
export type NamePreset = (typeof NAME_PRESETS)[number]

export function presetForName(name: string | undefined): NamePreset {
  if (!name) return 'Opphavet'
  return (NAME_PRESETS as readonly string[]).includes(name) ? (name as NamePreset) : 'Annet'
}

export interface BookingFormFieldError {
  name: string
  message: string
}

export interface BookingFormState {
  name: string
  numberOfPeople: number | undefined
  arrivalDate: string
  departureDate: string
}

export function validateBookingForm(state: BookingFormState): BookingFormFieldError[] {
  const errors: BookingFormFieldError[] = []
  const trimmedName = state.name.trim()

  if (!trimmedName) {
    errors.push({ name: 'name', message: 'Navn er påkrevd' })
  } else if (trimmedName.length > NAME_MAX_LENGTH) {
    errors.push({ name: 'name', message: `Navn kan være maks ${NAME_MAX_LENGTH} tegn` })
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
