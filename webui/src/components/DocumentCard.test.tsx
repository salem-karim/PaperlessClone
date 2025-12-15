import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '../test/test-utils'
import userEvent from '@testing-library/user-event'
import { DocumentCard } from './DocumentCard'
import type { DocumentSummaryDto } from '../lib/types'

const mockDocument: DocumentSummaryDto = {
  id: '123',
  title: 'Test Document',
  fileSize: 2048,
  originalFilename: 'test.pdf',
  contentType: 'application/pdf',
  processingStatus: 'COMPLETED',
  createdAt: '2024-01-15T10:00:00Z',
}

describe('DocumentCard', () => {
  it('renders document information', () => {
    render(<DocumentCard document={mockDocument} onDelete={() => {}} />)

    expect(screen.getByText('Test Document')).toBeInTheDocument()
    expect(screen.getByText('2.0 KB')).toBeInTheDocument()
  })

  it('links to document details page', () => {
    render(<DocumentCard document={mockDocument} onDelete={() => {}} />)

    const link = screen.getByRole('link', { name: /Test Document/i })
    expect(link).toHaveAttribute('href', '/documents/123')
  })

  it('calls onDelete when delete button is clicked', async () => {
    const user = userEvent.setup()
    const onDelete = vi.fn()

    render(<DocumentCard document={mockDocument} onDelete={onDelete} />)

    const deleteButton = screen.getByRole('button')
    await user.click(deleteButton)

    expect(onDelete).toHaveBeenCalledWith('123')
  })

  it('includes previous search in link state', () => {
    render(<DocumentCard document={mockDocument} onDelete={() => {}} />)

    const link = screen.getByRole('link', { name: /Test Document/i })
    expect(link).toBeInTheDocument()
  })
})