#include "../include/algorithm_body.h"
#include "../include/setting.h"
#include "../include/random.h"
#include "../include/functions.h"
#include "../include/body.h"
#include "../include/output.h"
#include <stdio.h>
#include <time.h>
#include <stdint.h>
#include <math.h>
#include <stdlib.h>

extern double c_x, c_y, c_z, s_x, s_y, s_z;
extern double omega, kappa, phi, d_x, d_y, d_z;
extern double xSampleSize, ySampleSize, zSampleSize;
extern double theta, beta, gammaValue, sU, sB, sR, sL, E_start, E_end;
extern int t, det1, det2, inType, intersectionFlag, i;
extern double rho, M, Nak;
extern double x, y, z, u, v, w;
extern double x_s, y_s, z_s, u_s, v_s, w_s, x_d, y_d, z_d, u_d, v_d, w_d;
extern double x_moved, y_moved, z_moved, R_s[9], R_d[9], cos_a, sin_a, cos_b, sin_b, cos_c, sin_c;
extern double s_ph, s_cs, s_is, s_sum;
extern double* s_list;
extern double E, linAbs, detArray[988][988], detArray_squared[988][988];
extern uint64_t N, num;
extern double freeTrial, a, e_n, e0;
extern double tx_l, tx_r, ty_l, ty_r, tz_l, tz_r, t_l, t_r;
extern time_t compTime;
extern int seed[10];
extern int mul_er[10];
extern double digit[10];
extern int aux, c0, c1, c2, c3, c4, c5, c6, c7, c8, c9;


int main_loop() {
	// �������� ������������ ����������
	for (num = 1; num <= N; num++) {
		// ������� �� ����� ������� �������� � ��������� �����
//		if (num == 1 || num == N || num % (N / 20) == 0) printf("%llu\\%llu------%d\n", num, N, (int)(time(NULL) - compTime));
		// ������ inType ��������� ��������
		inType = -1;

		// ����� ��������� ��������� ������ (����������, ����������� ��������, �������)
		startParams();

		// ����� ������� ��������������
		newSigma();

		// ��������� � �� �������
		cs_AbsToSamp();

		// ��������� ����� ����� ������ � �������
		intersectionPoint();

		// �������� ������������ ���� �������, ���� ����� ����� � �������
		if (intersectionFlag) {
			// ����� ����������� ��������� ����������
			linAbs = Nak * rho * s_sum / M;

			while (1) {
				// ���������� ��������� ������
				freeTrial = (-log(rnd128())) / linAbs;

				// ������������� ���������� ������
				x_s += freeTrial * u_s;
				y_s += freeTrial * v_s;
				z_s += freeTrial * w_s;

				// ���������� �������������, ���� ����� ������� � �������
				if (inBody()) {
					// ���������� ��� ������������
					interactingType();

					// ���� ��������� ����������, �� ���������� ������������� ����������
					if (inType == 0) break;
					// ���� ��������� ����������� ���������, ������������� ������ �������� ������
					else if (inType == 1) coherentScattering();
					// ���� ��������� ������������� ���������, ������������� ������ �������� ������,
					// ��� �������, ������� �������������� � ����������� ��������� ����������
					else {
						incoherentScattering();
						newSigma();
						linAbs = Nak * rho * s_sum / M;
					}
				}
				// ���� ����� ������� �� �������, �� ���������� ������������� ���� �������
				// � �������� ���������� ��������
				else break;
			}
		}

		// ����������� ����� ������ � ��������� (������������ ��� ���? ���� ��, �� � ����� �����)
		if (inType != 0) { // ���� ����� �� ���������� ��������, �� ���������
			// ��������� � �� ��������� �� ������� ��������� �������
			cs_SampToDet();

			// ��������� ����������� �� ���������
			imgOnDet();
		}
	}

	return 0;
}
int algorithm_body(int exp_count) {
	// ���������� ����� ������ ������ ���������
	compTime = time(NULL);

	// ��������� ������� ������
	setData();
	// ��������� ������ ��� �������� ������� (���������, �������� �����, ������� �������)
	setSampleMaterial();
//	printf("%lf\n", rho);

	// ���������� ���������� ����������
	defN();
	N *= exp_count;

	// ��������� ������� ���������
	rotateMatrices();

	// ��������� ��� ���������� ��������������� �����
	rndSeed(1000000000);

	// �������� ��������
	main_loop();

	free(s_list);

	output();

	return 0;
}