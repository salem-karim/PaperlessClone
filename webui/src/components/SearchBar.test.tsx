import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '../test/test-utils'
import userEvent from '@testing-library/user-event'
import { SearchBar } from './SearchBar'

describe('SearchBar', () => {
  it('renders input and button', () => {
    render(
      <SearchBar value="" onChange={() => {}} onSubmit={() => {}} />
    )

    expect(screen.getByPlaceholderText('Search documents…')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument()
  })

  it('calls onChange when typing', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()

    render(
      <SearchBar value="" onChange={onChange} onSubmit={() => {}} />
    )

    const input = screen.getByPlaceholderText('Search documents…')
    await user.type(input, 'test query')

    expect(onChange).toHaveBeenCalledWith('t')
    expect(onChange).toHaveBeenCalledTimes(10) // "test query" = 10 chars
  })

  it('calls onSubmit when form is submitted', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()

    render(
      <SearchBar value="test" onChange={() => {}} onSubmit={onSubmit} />
    )

    const button = screen.getByRole('button', { name: 'Search' })
    await user.click(button)

    expect(onSubmit).toHaveBeenCalledTimes(1)
  })

  it('calls onSubmit when Enter is pressed', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()

    render(
      <SearchBar value="test" onChange={() => {}} onSubmit={onSubmit} />
    )

    const input = screen.getByPlaceholderText('Search documents…')
    await user.type(input, '{Enter}')

    expect(onSubmit).toHaveBeenCalledTimes(1)
  })
})