#include "../include/random.h"
#include <math.h>
#include <stdio.h>

extern int seed[10];
extern int mul_er[10];
extern double digit[10];
extern int aux, c0, c1, c2, c3, c4, c5, c6, c7, c8, c9;

int rndSeed(int fig) {
	for (int i = 9; i >= 0; --i) {
		seed[i] = fig % 10;
		fig /= 10;
	}
	digit[0] = pow(2., -128.);
	digit[1] = pow(2., -115.);
	digit[2] = pow(2., -102.);
	digit[3] = pow(2., -89.);
	digit[4] = pow(2., -76.);
	digit[5] = pow(2., -63.);
	digit[6] = pow(2., -50.);
	digit[7] = pow(2., -37.);
	digit[8] = pow(2., -24.);
	digit[9] = pow(2., -11.);

	return 0;
}
double rnd128() {
	c0 = mul_er[0] * seed[0];
	c1 = mul_er[0] * seed[1] + mul_er[1] * seed[0];
	c2 = mul_er[0] * seed[2] + mul_er[1] * seed[1] + mul_er[2] * seed[0];
	c3 = mul_er[0] * seed[3] + mul_er[1] * seed[2] + mul_er[2] * seed[1] + mul_er[3] * seed[0];
	c4 = mul_er[0] * seed[4] + mul_er[1] * seed[3] + mul_er[2] * seed[2] + mul_er[3] * seed[1] + mul_er[4] * seed[0];
	c5 = mul_er[0] * seed[5] + mul_er[1] * seed[4] + mul_er[2] * seed[3] + mul_er[3] * seed[2] + mul_er[4] * seed[1] + mul_er[5] * seed[0];
	c6 = mul_er[0] * seed[6] + mul_er[1] * seed[5] + mul_er[2] * seed[4] + mul_er[3] * seed[3] + mul_er[4] * seed[2] + mul_er[5] * seed[1] + mul_er[6] * seed[0];
	c7 = mul_er[0] * seed[7] + mul_er[1] * seed[6] + mul_er[2] * seed[5] + mul_er[3] * seed[4] + mul_er[4] * seed[3] + mul_er[5] * seed[2] + mul_er[6] * seed[1] + mul_er[7] * seed[0];
	c8 = mul_er[0] * seed[8] + mul_er[1] * seed[7] + mul_er[2] * seed[6] + mul_er[3] * seed[5] + mul_er[4] * seed[4] + mul_er[5] * seed[3] + mul_er[6] * seed[2] + mul_er[7] * seed[1] + mul_er[8] * seed[0];
	c9 = mul_er[0] * seed[9] + mul_er[1] * seed[8] + mul_er[2] * seed[7] + mul_er[3] * seed[6] + mul_er[4] * seed[5] + mul_er[5] * seed[4] + mul_er[6] * seed[3] + mul_er[7] * seed[2] + mul_er[8] * seed[1] + mul_er[9] * seed[0];

	seed[0] = c0 - ((c0 >> 13) << 13);
	aux = c1 + (c0 >> 13);
	seed[1] = aux - ((aux >> 13) << 13);
	aux = c2 + (aux >> 13);
	seed[2] = aux - ((aux >> 13) << 13);
	aux = c3 + (aux >> 13);
	seed[3] = aux - ((aux >> 13) << 13);
	aux = c4 + (aux >> 13);
	seed[4] = aux - ((aux >> 13) << 13);
	aux = c5 + (aux >> 13);
	seed[5] = aux - ((aux >> 13) << 13);
	aux = c6 + (aux >> 13);
	seed[6] = aux - ((aux >> 13) << 13);
	aux = c7 + (aux >> 13);
	seed[7] = aux - ((aux >> 13) << 13);
	aux = c8 + (aux >> 13);
	seed[8] = aux - ((aux >> 13) << 13);
	aux = c9 + (aux >> 13);
	seed[9] = aux - ((aux >> 11) << 11);

	return seed[0] * digit[0] + seed[1] * digit[1] + seed[2] * digit[2] + seed[3] * digit[3] + seed[4] * digit[4] + seed[5] * digit[5] + seed[6] * digit[6] + seed[7] * digit[7] + seed[8] * digit[8] + seed[9] * digit[9];
}
