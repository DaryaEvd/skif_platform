#define _USE_MATH_DEFINES
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <windows.h>
#include <stdint.h>

// ��������� ��������������� �����
static int seed[10] = { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
static int mul_er[10] = { 1941, 1821, 3812, 1310, 68, 2906, 2335, 2609, 6859, 1999 };
static double digit[10];
static int aux, c0, c1, c2, c3, c4, c5, c6, c7, c8, c9;
static void rndSeed(int fig) {
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
}
static double rnd128() {
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


// ��� ����������� ���������� //

// ������� ������

// ����������� ����� ���������� (��, ��, ��), �� = �� * 1e+6
static double c_x = 0 * 1e+6, c_y = 3950 * 1e+6, c_z = 0.05 * 1e+6;
// ���������� �������
static double s_x = 0.05 * 1e+6, s_y = -0.05 * 1e+6, s_z = 0.05 * 1e+6; // ��������� ������ (��, ��, ��), �� = �� * 1e+6
static double omega = M_PI / 2, kappa = -M_PI / 7, phi = M_PI / 9; // ���� ������ (���������, ������� � ������. ��������) ��� ��������� ������� ��������� (������� ZXZ)
static double xSampleSize = 0.7 * 1e+6, ySampleSize = 0.3 * 1e+6, zSampleSize = 0.65 * 1e+6; // ������� �� ���� (��, ��, ��), �� = �� * 1e+6
// ���������� ���������
static double d_x = 0.5 * 1e+6, d_y = 55 * 1e+6, d_z = 0.24 * 1e+6; // ��������� ������ (��, ��, ��), �� = �� * 1e+6
static double theta = 0, beta = 0, gammaValue = 0; // ���� ������ (���������, ������� � ������. ��������) ��� ��������� ������� ��������� (������� ZXZ)
// ��������� ������� ���� (�������� ������� ���� �������� �� ����)
static double sU = 0.05 * 1e+6, sB = 0.05 * 1e+6, sR = 0.05 * 1e+6, sL = 0.05 * 1e+6;
// ������� ��� �������� ������� (������, ���� ���� �����) (k��)
static double E_start = 30, E_end = 30;
// �������� (����� ���������, �)
static int t = 10;

// ��������� �������� (� / ��^3)
static double ro = 8.94; // Cu - 8.94, Be - 1.85
// �������� ����� �������� (� / ����)
static double M = 63.546; // Cu - 63.546, Be - 9.012
// k = 1e-31 - ����������� ���������� ������ ��������� � ���������
// Na = 6,02214076 * 1e+23 (���� / ����) - ����� ��������
static double Nak = 6.02214076 * 1e-8; // �� ������������

// �������� ����������

// ���������� � �������� ������� (��)
static double x, y, z, u, v, w;
// ���������� ������� � ��������� ��������, �������� ��������� ����� �������� � �� ��������,
// ������� �������� � �� ������� (�� ����� ������) � ������� �������� � �� ���������,
// �������� � ������ ����� ������
static double x_s, y_s, z_s, u_s, v_s, w_s, x_d, y_d, z_d, u_d, v_d, w_d;
static double x_moved, y_moved, z_moved, R_s[9], R_d[9], cos_a, sin_a, cos_b, sin_b, cos_c, sin_c;
// ������� �������������� (���� / ����) (������. ����., �����. ����., �����. ����.), �� ����� � �������
static double s_ph, s_cs, s_is, s_sum, s_list[100];
// ������� (K��) � ����������� ��������� ���������� (��^-1)
static double E, linAbs;
// ����� ����������
static uint64_t N = 22000000000ULL;

// ���������� ��������� �� ��������
static int det1, det2;
// ��������
static double detArray[988][988];

// ���������� ������ �����-�����

// C�������� ������, ��� ��������������
static double freeTrial;
static int inType = -1;
// ��������� ��������
static double a;
// ������� ���� ���������
static double mu;
// ���������� ��� ������ ���������-�������
static double d, d1, d2, b, cos_phi, sin_phi, k1, k2;
// ������������� �� ������� ����� ��������� ������� ������ � ��� ������������� �������
static double e_n, e0;

// ��������������� ����������

// ��������� ��������� ��� ����� ����� � ����
double tx_l, tx_r, ty_l, ty_r, tz_l, tz_r, t_l, t_r;
// ���� ������� �������������� ������ � ����
int intersectionFlag;
// ��������
static int i = 0;
// ������� �������
static uint64_t num;
// ������ ��� ������ ����� � ���������
static char buffer[11];

// ���������� ������ ��������
static void rotateMatrices() {
	cos_a = cos(omega); sin_a = sin(omega);
	cos_b = cos(kappa); sin_b = sin(kappa);
	cos_c = cos(phi); sin_c = sin(phi);

	R_s[0] = cos_a * cos_b * cos_c - sin_a * sin_c;
	R_s[1] = -sin_a * cos_b * cos_c - cos_a * sin_c;
	R_s[2] = -sin_b * cos_c;
	R_s[3] = cos_a * cos_b * sin_c + sin_a * cos_c;
	R_s[4] = -sin_a * cos_b * sin_c + cos_a * cos_c;
	R_s[5] = -sin_b * sin_c;
	R_s[6] = cos_a * sin_b;
	R_s[7] = -sin_a * sin_b;
	R_s[8] = cos_b;

	cos_a = cos(theta); sin_a = sin(theta);
	cos_b = cos(beta); sin_b = sin(beta);
	cos_c = cos(gammaValue); sin_c = sin(gammaValue);

	R_d[0] = cos_a * cos_b * cos_c - sin_a * sin_c;
	R_d[1] = -sin_a * cos_b * cos_c - cos_a * sin_c;
	R_d[2] = -sin_b * cos_c;
	R_d[3] = cos_a * cos_b * sin_c + sin_a * cos_c;
	R_d[4] = -sin_a * cos_b * sin_c + cos_a * cos_c;
	R_d[5] = -sin_b * sin_c;
	R_d[6] = cos_a * sin_b;
	R_d[7] = -sin_a * sin_b;
	R_d[8] = cos_b;
}
// ������� �������
static void sigmaSet() {
	i = 0;
	while (!((s_list[4 * i] <= E) & (E < s_list[4 * (i + 1)]))) ++i;
	s_cs = s_list[4 * i + 1] + (s_list[4 * (i + 1) + 1] - s_list[4 * i + 1]) *
		((E - s_list[4 * i]) / (s_list[4 * (i + 1)] - s_list[4 * i]));
	s_is = s_list[4 * i + 2] + (s_list[4 * (i + 1) + 2] - s_list[4 * i + 2]) *
		((E - s_list[4 * i]) / (s_list[4 * (i + 1)] - s_list[4 * i]));
	s_ph = s_list[4 * i + 3] + (s_list[4 * (i + 1) + 3] - s_list[4 * i + 3]) *
		((E - s_list[4 * i]) / (s_list[4 * (i + 1)] - s_list[4 * i]));
	s_sum = s_ph + s_cs + s_is;
}
// �������� �� ��, ��� ������ ��������� ������ �������
static int inBody() {
	if ((fabs(x_s) <= xSampleSize / 2) && (fabs(y_s) <= ySampleSize / 2) && (fabs(z_s) <= zSampleSize / 2)) return 1;
	return 0;
}
// ������������� ���� ��������������
static void interactingType() {
	a = rnd128() * s_sum;
	if (a <= s_ph) inType = 0;
	else if (a <= s_ph + s_cs) inType = 1;
	else inType = 2;
}
// �������� �������� ��� ����������� ���������
static void coherentScattering() {
	// ������������� �������� ���� ���������
	a = rnd128();
	if (a <= 0.75) mu = ((8. * a) / 3.) - 1;
	else {
		if (8 * a - 7 <= 0) mu = -pow(7 - 8 * a, 1. / 3);
		else mu = pow(8 * a - 7, 1. / 3);
	}


	// ������� ���������-�������
	d = (v_s >= 0) - (v_s < 0);
	// ������������� �������� ��
	while (1) {
		cos_phi = 1 - 2 * rnd128();
		sin_phi = 1 - 2 * rnd128();
		k1 = cos_phi * cos_phi + sin_phi * sin_phi;
		if (k1 <= 1) {
			k2 = pow(k1, -0.5);
			cos_phi *= k2;
			sin_phi *= k2;
			break;
		}
	}

	k1 = pow(1 - mu * mu, 0.5);
	d1 = k1 * cos_phi;
	d2 = k1 * sin_phi;
	b = u_s * d1 + w_s * d2;

	u_s = u_s * (mu - b / (1 + fabs(v_s))) + d1;
	v_s = v_s * mu - d * b;
	w_s = w_s * (mu - b / (1 + fabs(v_s))) + d2;
}
// �������� ����������� �������� � ������� ��� ������������� ���������
static void incoherentScattering() {
	e_n = E / 511;

	// ������������� ����� ������� ������� ����������
	while (1) {
		e0 = e_n * (1 + 2 * e_n * rnd128()) / (1 + 2 * e_n);
		if (rnd128() * (1 + 2 * e_n + 1 / (1 + 2 * e_n)) <=
			(e0 / E + E / e0 + (1 / e_n - (511 / e0))
				* (2 + 1 / e_n - (511 / e0)))) {
			E = e0 * 511;
			break;
		}
	}

	// ������� ���� ���������
	mu = 1 - (511 / E) + (1 / e_n);

	// ������� ���������-�������
	d = (v_s >= 0) - (v_s < 0);

	// ������������� �������� ��
	while (1) {
		cos_phi = 1 - 2 * rnd128();
		sin_phi = 1 - 2 * rnd128();
		k1 = cos_phi * cos_phi + sin_phi * sin_phi;
		if (k1 <= 1) {
			k2 = pow(k1, -0.5);
			cos_phi *= k2;
			sin_phi *= k2;
			break;
		}
	}

	k1 = pow(1 - mu * mu, 0.5);
	d1 = k1 * cos_phi;
	d2 = k1 * sin_phi;
	b = u_s * d1 + w_s * d2;

	u_s = u_s * (mu - b / (1 + fabs(v_s))) + d1;
	v_s = v_s * mu - d * b;
	w_s = w_s * (mu - b / (1 + fabs(v_s))) + d2;
}

int main() {
	// ���������� ����� ������ ������ ���������
	compTime = time(NULL);

	// ��������� ������� �������
	FILE* s_file = fopen("crossSection_Cu.txt", "r");
	if (s_file == NULL) {
		printf("Sections opening error.");
		return 2;
	}
	i = 0;
	while (fgets(buffer, sizeof(buffer), s_file)) {
		s_list[i] = atof(buffer) * (1 + 999 * (i % 4 == 0));
		++i;
	}
	fclose(s_file);

	// ��������� ��� ����������
	rndSeed(1000000000);

	// ���������� ������� ����������� ����� � ����
	if (sU > -sB && sL > -sR) {
		if (sU > 0.35e+6) sU = 0.35e+6;
		if (sB > 0.35e+6) sB = 0.35e+6;
		if (sR > 5e+6) sR = 5e+6;
		if (sL > 5e+6) sL = 5e+6;
	}
	else if (sL > -sR) {
		sB = -sU;
	}
	else if (sU > -sB) {
		sL = -sR;
	}
	else {
		sB = -sU;
		sL = -sR;
	}
	k1 = (sR + sL) * (sU + sB) / 7e+12; // ��������� �������� ����������� ������� �����, ���������� ���� � ������������ �����

	// ���������� ����� �������
	N = round(N * t * k1); // ����� ������� * ����� ��������� * ��������� ��������

	// �������� ������������ ����������
	for (int count = 0; count < 1; ++count) {
		// ��������� ������� ���������
		rotateMatrices();
		for (num = 1; num <= N; ++num) {
			// ������� �� ����� ������� �������� � ��������� �����
			if (num == 1 || num == N || num % (N / 20) == 0) printf("%llu\\%llu------%d\n", num, N, (int)(time(NULL) - compTime));
			// ������ inType ��������� ��������
			inType = -1;

			// ���������� ������� ������ ������� ����������
			while (1) {
				d1 = (1 / ((E_end - E_start) + 0.0015));
				k1 = d1 * rnd128();
				k2 = (E_start - 0.0015) + (E_end - E_start + 0.003) * rnd128();
				if ((E_start <= k2) && (k2 <= E_end)) {
					E = k2;
					break;
				}
				else if ((E_start > k2) && (k1 <= (d1 / 0.0015) * (k2 - E_start + 0.0015))) {
					E = k2;
					break;
				}
				else if ((E_end < k2) && (k1 <= (d1 / 0.0015) * (E_end + 0.0015 - k2))) {
					E = k2;
					break;
				}
			}

			// ����� ������� ��������������
			sigmaSet();

			// ���������� ��������� ���������� ������� � ����������� �������������� �� ����
			x = -sR + (sL + sR) * rnd128();
			z = -sB + (sU + sB) * rnd128();
			y = 0;

			// ����� ��������� ������ ��������
			k1 = -0.01e-3 + (0.02e-3) * rnd128();
			k2 = 2 * M_PI * rnd128();
			u = k1 * cos(k2); v = cos(k1); w = k1 * sin(k2);

			// ��������� � �� �������
			x_moved = x - c_x;
			y_moved = y - c_y; // ��������� ������� � ��������� � ����� ���������� ��
			z_moved = z - c_z;

			x_s = R_s[0] * x_moved + R_s[1] * y_moved + R_s[2] * z_moved - s_x;
			y_s = R_s[3] * x_moved + R_s[4] * y_moved + R_s[5] * z_moved - s_y; // ��������� ������� � �� �������
			z_s = R_s[6] * x_moved + R_s[7] * y_moved + R_s[8] * z_moved - s_z;

			u_s = R_s[0] * u + R_s[1] * v + R_s[2] * w;
			v_s = R_s[3] * u + R_s[4] * v + R_s[5] * w; // �������� ������� � �� �������
			w_s = R_s[6] * u + R_s[7] * v + R_s[8] * w;

			// ���������� ���������� ��������� ������ � �������
			intersectionFlag = 1; // ���� ������� ���������

			// ���������� �������� ��������� t ��� x
			if (u_s == 0) {
				if (((-xSampleSize / 2 - x_s) <= 0) && (0 <= (xSampleSize / 2 - x_s))) {
					tx_l = -1e+15;
					tx_r = 1e+15;
				}
				else {
					intersectionFlag = 0;
				}
			}
			else if (u_s > 0) {
				tx_r = (xSampleSize / 2 - x_s) / u_s;
				tx_l = (-xSampleSize / 2 - x_s) / u_s;
			}
			else {
				tx_r = (-xSampleSize / 2 - x_s) / u_s;
				tx_l = (xSampleSize / 2 - x_s) / u_s;
			}
			// ���������� �������� ��������� t ��� y
			if (v_s == 0) {
				if (((-ySampleSize / 2 - y_s) <= 0) && (0 <= (ySampleSize / 2 - y_s))) {
					ty_l = -1e+15;
					ty_r = 1e+15;
				}
				else {
					intersectionFlag = 0;
				}
			}else if (v_s > 0) {
				ty_r = (ySampleSize / 2 - y_s) / v_s;
				ty_l = (-ySampleSize / 2 - y_s) / v_s;
			}
			else {
				ty_r = (-ySampleSize / 2 - y_s) / v_s;
				ty_l = (ySampleSize / 2 - y_s) / v_s;
			}
			// ���������� �������� ��������� t ��� z
			if (w_s == 0) {
				if (((-zSampleSize / 2 - z_s) <= 0) && (0 <= (zSampleSize / 2 - z_s))) {
					tz_l = -1e+15;
					tz_r = 1e+15;
				}
				else {
					intersectionFlag = 0;
				}
			}
			else if (w_s > 0) {
				tz_r = (zSampleSize / 2 - z_s) / w_s;
				tz_l = (-zSampleSize / 2 - z_s) / w_s;
			}
			else {
				tz_r = (-zSampleSize / 2 - z_s) / w_s;
				tz_l = (zSampleSize / 2 - z_s) / w_s;
			}

			// ���������� ����� ������� ��������� ��������� t ��� ���� ���������� �����
			t_l = tx_l;
			if (ty_l > t_l) t_l = ty_l;
			if (tz_l > t_l) t_l = tz_l;
			// ���������� ������ ������� ��������� ��������� t ��� ���� ���������� �����
			t_r = tx_r;
			if (ty_r < t_r) t_r = ty_r;
			if (tz_r < t_r) t_r = tz_r;

			// ���������� ���������� ��������� ������ � �������
			if (t_l > t_r) intersectionFlag = 0;
			else {
				x_s += t_l * u_s;
				y_s += t_l * v_s;
				z_s += t_l * w_s;
			}


			// �������� ������������ ���� �������, ���� ����� ����� � �������
			if (intersectionFlag) {
				// ����� ����������� ��������� ����������
				linAbs = Nak * ro * s_sum / M;

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
							sigmaSet();
							linAbs = Nak * ro * s_sum / M;
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
				x_moved = x_s + s_x;
				y_moved = y_s + s_y; // ��������� ������� � ��������� � ����� ���������� ��
				z_moved = z_s + s_z;

				x_d = (R_d[0] * R_s[0] + R_d[1] * R_s[1] + R_d[2] * R_s[2]) * x_moved
					+ (R_d[0] * R_s[3] + R_d[1] * R_s[4] + R_d[2] * R_s[5]) * y_moved
					+ (R_d[0] * R_s[6] + R_d[1] * R_s[7] + R_d[2] * R_s[8]) * z_moved - d_x;

				y_d = (R_d[3] * R_s[0] + R_d[4] * R_s[1] + R_d[5] * R_s[2]) * x_moved
					+ (R_d[3] * R_s[3] + R_d[4] * R_s[4] + R_d[5] * R_s[5]) * y_moved
					+ (R_d[3] * R_s[6] + R_d[4] * R_s[7] + R_d[5] * R_s[8]) * z_moved - d_y;  // ��������� ������� � �� ���������

				z_d = (R_d[6] * R_s[0] + R_d[7] * R_s[1] + R_d[8] * R_s[2]) * x_moved
					+ (R_d[6] * R_s[3] + R_d[7] * R_s[4] + R_d[8] * R_s[5]) * y_moved
					+ (R_d[6] * R_s[6] + R_d[7] * R_s[7] + R_d[8] * R_s[8]) * z_moved - d_z;

				u_d = (R_d[0] * R_s[0] + R_d[1] * R_s[1] + R_d[2] * R_s[2]) * u_s
					+ (R_d[0] * R_s[3] + R_d[1] * R_s[4] + R_d[2] * R_s[5]) * v_s
					+ (R_d[0] * R_s[6] + R_d[1] * R_s[7] + R_d[2] * R_s[8]) * w_s;

				v_d = (R_d[3] * R_s[0] + R_d[4] * R_s[1] + R_d[5] * R_s[2]) * u_s
					+ (R_d[3] * R_s[3] + R_d[4] * R_s[4] + R_d[5] * R_s[5]) * v_s
					+ (R_d[3] * R_s[6] + R_d[4] * R_s[7] + R_d[5] * R_s[8]) * w_s; // �������� ������� � �� ���������

				w_d = (R_d[6] * R_s[0] + R_d[7] * R_s[1] + R_d[8] * R_s[2]) * u_s
					+ (R_d[6] * R_s[3] + R_d[7] * R_s[4] + R_d[8] * R_s[5]) * v_s
					+ (R_d[6] * R_s[6] + R_d[7] * R_s[7] + R_d[8] * R_s[8]) * w_s;

				// ��������� ����������� �� ���������
				if (v_d > 0) { // ��� ��� �������� ���������� � ��������� Ox_d,z_d, ��� ������� ������ �������� ������, ������� �� ����
					t_l = -y_d / v_d; // ��������, ����������� ����� ����������� ���� ������ � ��������� ���������
					x_d += u_d * t_l;
					z_d += w_d * t_l;

					if ((fabs(x_d) <= 84.968e+6) && (fabs(z_d) <= 84.968e+6)) { // ������� ��������� � ��������
						det1 = (int)((84.968e+6 - x_d) / 172e+3); // ���������� �������� �������
						det2 = (int)((84.968e+6 - z_d) / 172e+3);
						detArray[det1][det2] += E;
					}
				}
			}
		}

		// ���������� ����������� � ����
		FILE* det = fopen("detector.txt", "w");
		if (det == NULL) {
			printf("Detector file opening error.");
			return 1;
		}
		fprintf(det, "%lf|%lf|%lf\n", c_x, c_y, c_z);
		fprintf(det, "%lf|%lf|%lf|%lf|%lf|%lf|%lf|%lf|%lf\n", s_x, s_y, s_z, omega, kappa, phi, xSampleSize, ySampleSize, zSampleSize);
		fprintf(det, "%lf|%lf|%lf|%lf|%lf|%lf\n", d_x, d_y, d_z, theta, beta, gammaValue);
		fprintf(det, "%lf|%lf|%lf|%lf\n", sU, sB, sR, sL);
		fprintf(det, "%lf|%lf|%d\n", E_start, E_end, t);
		for (det1 = 0; det1 < 988; ++det1) {
			for (det2 = 0; det2 < 988; ++det2) {
				fprintf(det, "%d|%d|%lf\n", det1, det2, detArray[det1][det2]);
			}
		}
		fclose(det);

		for (det1 = 0; det1 < 988; ++det1) {
			for (det2 = 0; det2 < 988; ++det2) {
				detArray[det1][det2] = 0;
			}
		}
		system("C:\\Diploma\\draft\\newv2.py");
	}

	return 0;
}
