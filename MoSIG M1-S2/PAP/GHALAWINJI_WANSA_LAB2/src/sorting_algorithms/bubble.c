#include <stdio.h>
#include <omp.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#include <stdbool.h>

#include <x86intrin.h>

#include "sorting.h"

/*
   bubble sort -- sequential, parallel --
*/

void swap(int *n1, int *n2)
{

    int temp = *n1;
    *n1 = *n2;
    *n2 = temp;
}
void sequential_bubble_sort(uint64_t *T, const uint64_t size)
{
    bool sorted;
    int temp;
    /* TODO: sequential implementation of bubble sort */
    do
    {
        sorted = true;
        for (int i = 0; i < size - 1; i++)
        {
            if (T[i] > T[i + 1])
            {
                swap(&T[i], &T[i + 1]);
                sorted = false;
            }
        }
    } while (sorted == false);
    return;
}

void parallel_bubble_sort(uint64_t *T, const uint64_t size)
{
    bool sorted = false;
    uint64_t nb_Threads = omp_get_max_threads();
    uint64_t s = size / nb_Threads;

    while (!sorted)
    {
        uint64_t i, j, k;
        sorted = true;

#pragma omp parallel for private(i, k)
        for (i = 0; i < nb_Threads; i++)
        {
            for (k = 0; k < s - 1; k++)
                if (T[i * s + k] > T[i * s + k + 1])
                {
                    swap(&T[i * s + k], &T[i * s + k + 1]);
                    sorted = false;
                }
        }
        // swapping chunk borders values
        for (j = 1; j < nb_Threads; j++)
        {
            if (T[j * s - 1] > T[j * s])
            {
                swap(&T[j * s - 1], &T[j * s]);
                sorted = false;
            }
        }
    }

    return;
}

int main(int argc, char **argv)
{
    struct timespec begin, end;
    double seconds;
    double nanoseconds;

    unsigned int exp;

    /* the program takes one parameter N which is the size of the array to
       be sorted. The array will have size 2^N */
    if (argc != 2)
    {
        fprintf(stderr, "bubble.run N \n");
        exit(-1);
    }

    uint64_t N = 1 << (atoi(argv[1]));
    /* the array to be sorted */
    uint64_t *X = (uint64_t *)malloc(N * sizeof(uint64_t));

    printf("--> Sorting an array of size %u\n", N);
#ifdef RINIT
    printf("--> The array is initialized randomly\n");
#endif

    for (exp = 0; exp < NBEXPERIMENTS; exp++)
    {
#ifdef RINIT
        init_array_random(X, N);
#else
        init_array_sequence(X, N);
#endif

        clock_gettime(CLOCK_MONOTONIC, &begin);

        sequential_bubble_sort(X, N);

        clock_gettime(CLOCK_MONOTONIC, &end);

        seconds = end.tv_sec - begin.tv_sec;
        nanoseconds = end.tv_nsec - begin.tv_nsec;

        experiments[exp] = seconds + nanoseconds * 1e-9;

        /* verifying that X is properly sorted */
#ifdef RINIT
        if (!is_sorted(X, N))
        {
            print_array(X, N);
            fprintf(stderr, "ERROR: the sequential sorting of the array failed\n");
            exit(-1);
        }
#else
        if (!is_sorted_sequence(X, N))
        {
            print_array(X, N);
            fprintf(stderr, "ERROR: the sequential sorting of the array failed\n");
            exit(-1);
        }
#endif
    }

    printf("\n bubble serial \t\t\t %.3lf seconds\n\n", average_time());

    for (exp = 0; exp < NBEXPERIMENTS; exp++)
    {
#ifdef RINIT
        init_array_random(X, N);
#else
        init_array_sequence(X, N);
#endif

        clock_gettime(CLOCK_MONOTONIC, &begin);

        parallel_bubble_sort(X, N);

        clock_gettime(CLOCK_MONOTONIC, &end);

        seconds = end.tv_sec - begin.tv_sec;
        nanoseconds = end.tv_nsec - begin.tv_nsec;

        experiments[exp] = seconds + nanoseconds * 1e-9;

        /* verifying that X is properly sorted */
#ifdef RINIT
        if (!is_sorted(X, N))
        {
            print_array(X, N);
            fprintf(stderr, "ERROR: the parallel sorting of the array failed\n");
            exit(-1);
        }
#else
        if (!is_sorted_sequence(X, N))
        {
            print_array(X, N);
            fprintf(stderr, "ERROR: the parallel sorting of the array failed\n");
            exit(-1);
        }
#endif
    }

    printf("\n bubble parallel \t\t %.3lf seconds\n\n", average_time());

    /* print_array (X, N) ; */

    /* before terminating, we run one extra test of the algorithm */
    uint64_t *Y = (uint64_t *)malloc(N * sizeof(uint64_t));
    uint64_t *Z = (uint64_t *)malloc(N * sizeof(uint64_t));

#ifdef RINIT
    init_array_random(Y, N);
#else
    init_array_sequence(Y, N);
#endif

    memcpy(Z, Y, N * sizeof(uint64_t));

    sequential_bubble_sort(Y, N);
    parallel_bubble_sort(Z, N);

    if (!are_vector_equals(Y, Z, N))
    {
        fprintf(stderr, "ERROR: sorting with the sequential and the parallel algorithm does not give the same result\n");
        exit(-1);
    }

    free(X);
    free(Y);
    free(Z);
}