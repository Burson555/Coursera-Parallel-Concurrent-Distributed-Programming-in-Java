package edu.coursera.concurrent;

import edu.rice.pcdp.Actor;
import edu.rice.pcdp.PCDP;

/**
 * An actor-based implementation of the Sieve of Eratosthenes.
 *
 * TODO Fill in the empty SieveActorActor actor class below and use it from
 * countPrimes to determin the number of primes <= limit.
 */
public final class SieveActor extends Sieve {
    /**
     * {@inheritDoc}
     *
     * TODO Use the SieveActorActor class to calculate the number of primes <=
     * limit in parallel. You might consider how you can model the Sieve of
     * Eratosthenes as a pipeline of actors, each corresponding to a single
     * prime number.
     */
    @Override
    public int countPrimes(final int limit) {
        if (limit < 2) return 0;
        final SieveActorActor head_actor = new SieveActorActor(2);
        PCDP.finish(() -> {
            for (int i = 3; i <= limit; i += 2) head_actor.send(i);
        });
        int count = 0;
        SieveActorActor iter_actor = head_actor;
        while (iter_actor != null) {
            count += iter_actor.getNumPrime();
            iter_actor = iter_actor.getNextActor();
        }
        return count;
    }

    /**
     * An actor class that helps implement the Sieve of Eratosthenes in
     * parallel.
     */
    public static final class SieveActorActor extends Actor {

        private static final int MAX_NUM_PRIME = 32;
        private final int[] prime_list;
        private int num_prime;
        private SieveActorActor next_actor;

        public SieveActorActor(int local_prime) {
            this.prime_list = new int[MAX_NUM_PRIME];
            this.prime_list[0] = local_prime;
            this.num_prime = 1;
            this.next_actor = null;
        }

        public int getNumPrime() {
            return num_prime;
        }

        public SieveActorActor getNextActor() {
            return next_actor;
        }
        /**
         * Process a single message sent to this actor.
         *
         * TODO complete this method.
         *
         * @param msg Received message
         */
        @Override
        public void process(final Object msg) {
            final int candidate = (Integer) msg;
            if (!isLocalPrime(candidate)) {
                if (num_prime < MAX_NUM_PRIME) {
                    prime_list[num_prime++] = candidate;
                } else if (next_actor == null) {
                    next_actor = new SieveActorActor(candidate);
                } else {
                    next_actor.send(candidate);
                }
            }
        }

        private boolean isLocalPrime(int candidate) {
            final int temp = num_prime;
            for (int i = 0; i < temp; i++) {
                if (candidate % prime_list[i] == 0) {
                    return true;
                }
            }
            return false;
        }
    }
}


// /**
//  * An actor-based implementation of the Sieve of Eratosthenes.
//  *
//  * TODO Fill in the empty SieveActorActor actor class below and use it from
//  * countPrimes to determin the number of primes <= limit.
//  */
// public final class SieveActor extends Sieve {
//     /**
//      * {@inheritDoc}
//      *
//      * TODO Use the SieveActorActor class to calculate the number of primes <=
//      * limit in parallel. You might consider how you can model the Sieve of
//      * Eratosthenes as a pipeline of actors, each corresponding to a single
//      * prime number.
//      */
//     @Override
//     public int countPrimes(final int limit) {
//         if (limit < 2) return 0;
//         final SieveActorActor head_actor = new SieveActorActor(2, 0);
//         PCDP.finish(() -> {
//             for (int i = 3; i <= limit; i += 2) head_actor.send(i);
//         }); // WHY head_actor has to be ginal here?
//         SieveActorActor iter_actor = head_actor;
//         while (iter_actor.getNextActor() != null) 
//             iter_actor = iter_actor.getNextActor();
//         return iter_actor.getNumPrime();
//     }

//     /**
//      * An actor class that helps implement the Sieve of Eratosthenes in
//      * parallel.
//      */
//     public static final class SieveActorActor_B extends Actor {

//         private final int local_prime;
//         private int num_prime;
//         private SieveActorActor_B next_actor;

//         public SieveActorActor_B(int local_prime, int num_prime_prev) {
//             this.local_prime = local_prime;
//             this.num_prime = num_prime_prev+1;
//             this.next_actor = null;
//         }

//         public int getNumPrime() {
//             return num_prime;
//         }

//         public SieveActorActor_B getNextActor() {
//             return next_actor;
//         }
//         /**
//          * Process a single message sent to this actor.
//          *
//          * TODO complete this method.
//          *
//          * @param msg Received message
//          */
//         @Override
//         public void process(final Object msg) {
//             final int candidate = (Integer) msg;
//             if (candidate % local_prime != 0) {
//                 if (next_actor == null)
//                     next_actor = new SieveActorActor_B(candidate, num_prime);
//                 next_actor.send(candidate);
//             }
//         }
//     }
// }
