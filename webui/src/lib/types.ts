export type DocumentDto = {
  id: string;
  title: string;
};

type SuccessResult<T> = readonly [T, null];

type ErrorResult<E = Error> = readonly [null, E];

export type Result<T, E = Error> = SuccessResult<T> | ErrorResult<E>;
