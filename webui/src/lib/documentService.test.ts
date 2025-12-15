import { describe, it, expect, beforeEach, vi } from 'vitest'
import { getDocuments, createDocument, deleteDocument } from './documentService'
import type { DocumentSummaryDto } from './types'

describe('documentService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getDocuments', () => {
    it('fetches documents successfully', async () => {
      const mockDocs: DocumentSummaryDto[] = [
        {
          id: '1',
          title: 'Test Doc',
          fileSize: 1024,
          originalFilename: 'test.pdf',
          contentType: 'application/pdf',
          processingStatus: 'COMPLETED',
          createdAt: '2024-01-15T10:00:00Z',
        },
      ]

      globalThis.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockDocs,
      }) as typeof fetch

      const result = await getDocuments()
      expect(result).toEqual(mockDocs)
      expect(fetch).toHaveBeenCalledWith('/api/v1/documents')
    })

    it('throws error on failed request', async () => {
      globalThis.fetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
      }) as typeof fetch

      await expect(getDocuments()).rejects.toThrow('HTTP 500')
    })
  })

  describe('deleteDocument', () => {
    it('deletes document successfully', async () => {
      globalThis.fetch = vi.fn().mockResolvedValue({
        ok: true,
      }) as typeof fetch

      await deleteDocument('123')
      expect(fetch).toHaveBeenCalledWith('/api/v1/documents/123', {
        method: 'DELETE',
      })
    })
  })

  describe('createDocument', () => {
    it('creates document with FormData', async () => {
      const mockFile = new File(['content'], 'test.pdf', {
        type: 'application/pdf',
      })
      const mockDoc: DocumentSummaryDto = {
        id: '1',
        title: 'New Doc',
        fileSize: 1024,
        originalFilename: 'test.pdf',
        contentType: 'application/pdf',
        processingStatus: 'PENDING',
        createdAt: new Date().toISOString(),
      }

      globalThis.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockDoc,
      }) as typeof fetch

      const result = await createDocument({
        title: 'New Doc',
        file: mockFile,
      })

      expect(result).toEqual(mockDoc)
      expect(fetch).toHaveBeenCalledWith(
        '/api/v1/documents',
        expect.objectContaining({
          method: 'POST',
          body: expect.any(FormData),
        })
      )
    })
  })
})