import { describe, expect, it } from 'vitest'
import type { Session } from '~/types/booking'
import { sessionJustExpired } from './session'

function session(authenticated: boolean): Session {
  return {
    authenticated,
    name: authenticated ? 'Some User' : null,
    isAdmin: false,
    hasAccess: authenticated,
    group: null
  }
}

describe('sessionJustExpired', () => {
  it('fires when a logged-in session becomes anonymous', () => {
    expect(sessionJustExpired(session(true), session(false))).toBe(true)
  })

  it('does not fire while the session stays logged in', () => {
    expect(sessionJustExpired(session(true), session(true))).toBe(false)
  })

  it('does not fire for a visitor who was never logged in', () => {
    expect(sessionJustExpired(session(false), session(false))).toBe(false)
  })

  it('does not fire on the initial load, when there is no previous value', () => {
    expect(sessionJustExpired(undefined, session(false))).toBe(false)
    expect(sessionJustExpired(null, session(true))).toBe(false)
  })

  it('fires when the session data disappears entirely', () => {
    expect(sessionJustExpired(session(true), null)).toBe(true)
    expect(sessionJustExpired(session(true), undefined)).toBe(true)
  })

  it('does not fire on logging in', () => {
    expect(sessionJustExpired(session(false), session(true))).toBe(false)
  })
})
