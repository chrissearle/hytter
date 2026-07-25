import { describe, expect, it } from 'vitest'
import { NAME_MAX_LENGTH, validateBookingForm } from './bookingForm'

function validState(overrides: Partial<Parameters<typeof validateBookingForm>[0]> = {}) {
  return {
    name: 'Naboene',
    requiresName: true,
    numberOfPeople: 2,
    arrivalDate: '2026-06-01',
    departureDate: '2026-06-05',
    ...overrides
  }
}

describe('validateBookingForm', () => {
  it('has no errors for valid input', () => {
    expect(validateBookingForm(validState())).toEqual([])
  })

  it('requires a non-blank name when the free-text field is shown', () => {
    expect(validateBookingForm(validState({ name: '   ' }))).toContainEqual({
      name: 'name',
      message: 'Navn er påkrevd'
    })
  })

  it('ignores a blank name when the name comes from the backend', () => {
    expect(validateBookingForm(validState({ name: '', requiresName: false }))).toEqual([])
  })

  it('rejects a name over the max length', () => {
    expect(
      validateBookingForm(validState({ name: 'a'.repeat(NAME_MAX_LENGTH + 1) }))
    ).toContainEqual({
      name: 'name',
      message: `Navn kan være maks ${NAME_MAX_LENGTH} tegn`
    })
  })

  it('accepts a name at exactly the max length', () => {
    expect(validateBookingForm(validState({ name: 'a'.repeat(NAME_MAX_LENGTH) }))).toEqual([])
  })

  it('requires at least one person', () => {
    expect(validateBookingForm(validState({ numberOfPeople: 0 }))).toContainEqual({
      name: 'numberOfPeople',
      message: 'Antall personer må være minst 1'
    })
  })

  it('requires numberOfPeople when undefined', () => {
    expect(validateBookingForm(validState({ numberOfPeople: undefined }))).toContainEqual({
      name: 'numberOfPeople',
      message: 'Antall personer må være minst 1'
    })
  })

  it('requires an arrival date', () => {
    expect(validateBookingForm(validState({ arrivalDate: '' }))).toContainEqual({
      name: 'arrivalDate',
      message: 'Ankomstdato er påkrevd'
    })
  })

  it('requires a departure date', () => {
    expect(validateBookingForm(validState({ departureDate: '' }))).toContainEqual({
      name: 'departureDate',
      message: 'Avreisedato er påkrevd'
    })
  })

  it('rejects a departure date before the arrival date', () => {
    expect(
      validateBookingForm(validState({ arrivalDate: '2026-06-05', departureDate: '2026-06-01' }))
    ).toContainEqual({
      name: 'departureDate',
      message: 'Avreise kan ikke være før ankomst'
    })
  })

  it('accepts a same-day arrival and departure', () => {
    expect(
      validateBookingForm(validState({ arrivalDate: '2026-06-05', departureDate: '2026-06-05' }))
    ).toEqual([])
  })

  it('reports every violated field at once', () => {
    const errors = validateBookingForm({
      name: '',
      requiresName: true,
      numberOfPeople: 0,
      arrivalDate: '',
      departureDate: ''
    })

    expect(errors.map((e) => e.name)).toEqual([
      'name',
      'numberOfPeople',
      'arrivalDate',
      'departureDate'
    ])
  })
})
