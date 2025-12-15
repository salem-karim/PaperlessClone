import { describe, it, expect } from 'vitest'
import { formatFileSize, formatDate } from './utils'

describe('formatFileSize', () => {
  it('formats bytes correctly', () => {
    expect(formatFileSize(500)).toBe('500 B')
    expect(formatFileSize(1024)).toBe('1.0 KB')
    expect(formatFileSize(1048576)).toBe('1.0 MB')
    expect(formatFileSize(1073741824)).toBe('1.00 GB')
  })

  it('rounds to appropriate decimal places', () => {
    expect(formatFileSize(1536)).toBe('1.5 KB')
    expect(formatFileSize(2621440)).toBe('2.5 MB')
  })
})

describe('formatDate', () => {
  it('formats date strings correctly', () => {
    const date = '2024-01-15T10:30:00Z'
    const formatted = formatDate(date)
    expect(formatted).toMatch(/Jan|15|2024/)
  })
})