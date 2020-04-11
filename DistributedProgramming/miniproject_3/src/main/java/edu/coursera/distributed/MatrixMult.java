package edu.coursera.distributed;

import edu.coursera.distributed.util.MPI;
import edu.coursera.distributed.util.MPI.MPIException;

/**
 * A wrapper class for a parallel, MPI-based matrix multiply implementation.
 */
public class MatrixMult {
    /**
     * A parallel implementation of matrix multiply using MPI to express SPMD
     * parallelism. In particular, this method should store the output of
     * multiplying the matrices a and b into the matrix c.
     *
     * This method is called simultaneously by all MPI ranks in a running MPI
     * program. For simplicity MPI_Init has already been called, and
     * MPI_Finalize should not be called in parallelMatrixMultiply.
     *
     * On entry to parallelMatrixMultiply, the following will be true of a, b,
     * and c:
     *
     *   1) The matrix a will only be filled with the input values on MPI rank
     *      zero. Matrix a on all other ranks will be empty (initialized to all
     *      zeros).
     *   2) Likewise, the matrix b will only be filled with input values on MPI
     *      rank zero. Matrix b on all other ranks will be empty (initialized to
     *      all zeros).
     *   3) Matrix c will be initialized to all zeros on all ranks.
     *
     * Upon returning from parallelMatrixMultiply, the following must be true:
     *
     *   1) On rank zero, matrix c must be filled with the final output of the
     *      full matrix multiplication. The contents of matrix c on all other
     *      ranks are ignored.
     *
     * Therefore, it is the responsibility of this method to distribute the
     * input data in a and b across all MPI ranks for maximal parallelism,
     * perform the matrix multiply in parallel, and finally collect the output
     * data in c from all ranks back to the zeroth rank. You may use any of the
     * MPI APIs provided in the mpi object to accomplish this.
     *
     * A reference sequential implementation is provided below, demonstrating
     * the use of the Matrix class's APIs.
     *
     * @param a Input matrix
     * @param b Input matrix
     * @param c Output matrix
     * @param mpi MPI object supporting MPI APIs
     * @throws MPIException On MPI error. It is not expected that your
     *                      implementation should throw any MPI errors during
     *                      normal operation.
     */
    public static void parallelMatrixMultiply(Matrix a, Matrix b, Matrix c,
            final MPI mpi) throws MPIException {

        final int my_rank = mpi.MPI_Comm_rank(mpi.MPI_COMM_WORLD);
        final int num_rank = mpi.MPI_Comm_size(mpi.MPI_COMM_WORLD);

        final int num_rows = c.getNRows();
        final int num_cols = c.getNCols();
        final int chunk_size = (num_rows-1)/num_rank + 1;
        final int start_row = my_rank * chunk_size;
        final int end_row = Integer.min(num_rows, (my_rank+1)*chunk_size);

        // fork send the data needed:   a by point-to-point connection
        //                              b by broadcast
        if (my_rank == 0) {
            for (int i = 1; i < num_rank; i++) {
                final int rank_start_row = i*chunk_size;
                final int rank_end_row = Integer.min(num_rows, (i+1)*chunk_size);

                final int row_offset = rank_start_row * a.getNCols();
                final int num_elements = (rank_end_row-rank_start_row) * a.getNCols();

                mpi.MPI_Isend(a.getValues(), 
                                row_offset, num_elements, 
                                i, i, mpi.MPI_COMM_WORLD);
            }
            mpi.MPI_Bcast(b.getValues(), 
                            0, b.getNRows()*b.getNCols(), 
                            0, mpi.MPI_COMM_WORLD);
        } else {
            MPI.MPI_Request temp = mpi.MPI_Irecv(a.getValues(), 
                            start_row*a.getNCols(), 
                            (end_row-start_row)*a.getNCols(),
                            0, my_rank, mpi.MPI_COMM_WORLD);
            mpi.MPI_Bcast(b.getValues(), 
                            0, b.getNRows()*b.getNCols(), 
                            0, mpi.MPI_COMM_WORLD);
            mpi.MPI_Wait(temp);
        }

        // calculation
        for (int i = start_row; i < end_row; i++) {
            for (int j = 0; j < c.getNCols(); j++) {
                c.set(i, j, 0);
                for (int k = 0; k < b.getNRows(); k++) {
                    c.incr(i, j, a.get(i, k) * b.get(k, j));
                }
            }
        }

        // fork collect of data
        if (my_rank == 0) {
            MPI.MPI_Request requests[] = new MPI.MPI_Request[num_rank-1];
            for (int i = 1; i < num_rank; i++) {
                final int rank_start_row = i*chunk_size;
                final int rank_end_row = Integer.min(num_rows, (i+1)*chunk_size);

                final int row_offset = rank_start_row * num_cols;
                final int num_elements = (rank_end_row-rank_start_row) * num_cols;

                requests[i-1] = mpi.MPI_Irecv(c.getValues(), row_offset, num_elements,
                                i, i, mpi.MPI_COMM_WORLD);
            }
            mpi.MPI_Waitall(requests);
        } else {
            mpi.MPI_Send(c.getValues(), start_row*num_cols, 
                (end_row-start_row)*num_cols, 0, my_rank, mpi.MPI_COMM_WORLD);
        }
    }
}
