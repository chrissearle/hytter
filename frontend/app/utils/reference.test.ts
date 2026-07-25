import { describe, expect, it } from 'vitest'
import { hutDisplayName, nameTypeItem, needsNameInput } from './reference'
import type { Reference } from '~/types/booking'

const reference: Reference = {
  huts: [
    { value: 'HULDREBAKKEN', displayName: 'Huldrebakken' },
    { value: 'TROLLHAUGEN', displayName: 'Trollhaugen' },
    { value: 'TENT_HAMMOCK', displayName: 'Telt/hengekøye' }
  ],
  nameTypes: [
    { value: 'OPPHAVET', displayName: 'Opphavet', isFreeText: false },
    { value: 'SORKISRAMPEN', displayName: 'Sørkisrampen', isFreeText: false },
    { value: 'HA12', displayName: 'HA12', isFreeText: false },
    { value: 'PERSONAL', displayName: 'Personlig', isFreeText: true },
    { value: 'OTHER', displayName: 'Annet', isFreeText: true }
  ]
}

describe('hutDisplayName', () => {
  it('maps a hut value to its Bokmål label', () => {
    expect(hutDisplayName(reference, 'TENT_HAMMOCK')).toBe('Telt/hengekøye')
  })

  it('falls back to the raw value while reference data is loading', () => {
    expect(hutDisplayName(null, 'TROLLHAUGEN')).toBe('TROLLHAUGEN')
  })
})

describe('nameTypeItem', () => {
  it('finds the entry for a name type', () => {
    expect(nameTypeItem(reference, 'HA12')?.displayName).toBe('HA12')
  })

  it('returns undefined when there is no reference data', () => {
    expect(nameTypeItem(undefined, 'HA12')).toBeUndefined()
  })
})

describe('needsNameInput', () => {
  it('is false for a fixed group', () => {
    expect(needsNameInput(reference, 'OPPHAVET', false)).toBe(false)
  })

  it('is true for Annet whether or not you are logged in', () => {
    expect(needsNameInput(reference, 'OTHER', false)).toBe(true)
    expect(needsNameInput(reference, 'OTHER', true)).toBe(true)
  })

  it('is true for a personal booking by an anonymous visitor', () => {
    expect(needsNameInput(reference, 'PERSONAL', false)).toBe(true)
  })

  it('is false for a personal booking by a logged-in user', () => {
    expect(needsNameInput(reference, 'PERSONAL', true)).toBe(false)
  })

  it('is false while reference data is still loading', () => {
    expect(needsNameInput(null, 'OTHER', false)).toBe(false)
  })
})
