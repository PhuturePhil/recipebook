import { test } from 'node:test'
import assert from 'node:assert/strict'
import { dropdownPlacement, DROPDOWN_MAX_HEIGHT } from './dropdownPlacement.js'

test('opens below with the full height when there is room', () => {
  assert.deepEqual(
    dropdownPlacement({ fieldTop: 300, fieldBottom: 340, topLimit: 80, bottomLimit: 600 }),
    { up: false, maxHeight: DROPDOWN_MAX_HEIGHT }
  )
})

test('shrinks below while enough room is left', () => {
  assert.deepEqual(
    dropdownPlacement({ fieldTop: 300, fieldBottom: 340, topLimit: 80, bottomLimit: 490 }),
    { up: false, maxHeight: 150 }
  )
})

test('flips up above the sticky buttons or the keyboard', () => {
  assert.deepEqual(
    dropdownPlacement({ fieldTop: 420, fieldBottom: 460, topLimit: 80, bottomLimit: 520 }),
    { up: true, maxHeight: DROPDOWN_MAX_HEIGHT }
  )
  assert.deepEqual(
    dropdownPlacement({ fieldTop: 230, fieldBottom: 270, topLimit: 80, bottomLimit: 330 }),
    { up: true, maxHeight: 150 }
  )
})

test('stays below when neither side has more room', () => {
  const placement = dropdownPlacement({ fieldTop: 100, fieldBottom: 140, topLimit: 80, bottomLimit: 180 })
  assert.equal(placement.up, false)
  assert.equal(placement.maxHeight, 60)
})
