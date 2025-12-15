import 'vitest'
import type { TestingLibraryMatchers } from '@testing-library/jest-dom/matchers'

declare module 'vitest' {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  type Assertion<T = any> = TestingLibraryMatchers<typeof expect.stringContaining, T>
  
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  type AsymmetricMatchersContaining = TestingLibraryMatchers<typeof expect.stringContaining, any>
}