import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '../test/test-utils'
import { DocumentList } from './DocumentList'
import type { DocumentSummaryDto } from '../lib/types'

const mockDocuments: DocumentSummaryDto[] = [
  {
    id: '1',
    title: 'Document 1',
    fileSize: 1024,
    originalFilename: 'doc1.pdf',
    contentType: 'application/pdf',
    processingStatus: 'COMPLETED',
    createdAt: '2024-01-15T10:00:00Z',
  },
  {
    id: '2',
    title: 'Document 2',
    fileSize: 2048,
    originalFilename: 'doc2.pdf',
    contentType: 'application/pdf',
    processingStatus: 'PENDING',
    createdAt: '2024-01-16T10:00:00Z',
  },
]

describe('DocumentList', () => {
  it('renders all documents', () => {
    render(<DocumentList documents={mockDocuments} onDelete={() => {}} />)

    expect(screen.getByText('Document 1')).toBeInTheDocument()
    expect(screen.getByText('Document 2')).toBeInTheDocument()
  })

  it('renders empty list when no documents', () => {
    const { container } = render(
      <DocumentList documents={[]} onDelete={() => {}} />
    )

    expect(container.querySelector('.grid')?.children).toHaveLength(0)
  })

  it('passes onDelete to each card', () => {
    const onDelete = vi.fn()
    render(<DocumentList documents={mockDocuments} onDelete={onDelete} />)

    const deleteButtons = screen.getAllByRole('button')
    expect(deleteButtons).toHaveLength(2)
  })
})