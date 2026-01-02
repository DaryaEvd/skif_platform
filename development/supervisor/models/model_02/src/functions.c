#define _USE_MATH_DEFINES
#include "../include/functions.h"
#include "../include/random.h"
#include "../include/variables.h"
#include <math.h>
#include <stdint.h>
#include <stdio.h>

extern int i, t, inType, det1, det2;
extern double* s_list;
extern double s_cs, s_is, s_ph, s_sum, E;
extern double a, d, mu, cos_phi, sin_phi;
extern double k1, k2, d1, d2, b;
extern double sU, sB, sR, sL;
extern uint64_t N;
extern double x, y, z, u, v, w, E_start, E_end;
extern double c_x, c_y, c_z, s_x, s_y, s_z, d_x, d_y, d_z;
extern double x_s, y_s, z_s, u_s, v_s, w_s;
extern double x_d, y_d, z_d, u_d, v_d, w_d;
extern double x_moved, y_moved, z_moved, e0, e_n;
extern double R_s[9], R_d[9];
extern int intersectionFlag;
extern double xSampleSize, ySampleSize, zSampleSize;
extern double tx_r, tx_l, ty_r, ty_l, tz_r, tz_l, t_r, t_l;
extern double detArray[988][988], detArray_squared[988][988];
extern double max_dA, min_dA;


int newSigma() {
	i = 0;
	while (!((s_list[4 * i] <= E) & (E < s_list[4 * (i + 1)]))) ++i;

	s_cs = s_list[4 * i + 1] + (s_list[4 * (i + 1) + 1] - s_list[4 * i + 1]) *
		((E - s_list[4 * i]) / (s_list[4 * (i + 1)] - s_list[4 * i]));
	s_is = s_list[4 * i + 2] + (s_list[4 * (i + 1) + 2] - s_list[4 * i + 2]) *
		((E - s_list[4 * i]) / (s_list[4 * (i + 1)] - s_list[4 * i]));
	s_ph = s_list[4 * i + 3] + (s_list[4 * (i + 1) + 3] - s_list[4 * i + 3]) *
		((E - s_list[4 * i]) / (s_list[4 * (i + 1)] - s_list[4 * i]));
	s_sum = s_ph + s_cs + s_is;

	return 0;
}

int interactingType() {
	a = rnd128() * s_sum;
	if (a <= s_ph) inType = 0;
	else if (a <= s_ph + s_cs) inType = 1;
	else inType = 2;

	return 0;
}

int coherentScattering() {
	// Моделирование косинуса угла рассеяния
	a = rnd128();
	if (a <= 0.75) mu = ((8. * a) / 3.) - 1;
	else {
		if (8 * a - 7 <= 0) mu = -pow(7 - 8 * a, 1. / 3);
		else mu = pow(8 * a - 7, 1. / 3);
	}


	// Формулы Субботина-Ченцова 
	d = (v_s >= 0) - (v_s < 0);
	// Моделирование косинуса фи
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

	return 0;
}

int incoherentScattering() {
	e_n = E / 511;

	// Моделирование новой энергии методом исключения
	while (1) {
		e0 = e_n * (1 + 2 * e_n * rnd128()) / (1 + 2 * e_n);
		if (rnd128() * (1 + 2 * e_n + 1 / (1 + 2 * e_n)) <=
			(e0 / E + E / e0 + (1 / e_n - (511 / e0))
				* (2 + 1 / e_n - (511 / e0)))) {
			E = e0 * 511;
			break;
		}
	}

	// Косинус угла рассеяния
	mu = 1 - (511 / E) + (1 / e_n);

	// Формулы Субботина-Ченцова 
	d = (v_s >= 0) - (v_s < 0);

	// Моделирование косинуса фи
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

	return 0;
}

int defN() {
	// Определяем область пересечения пучка и щели
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
	k1 = (sR + sL) * (sU + sB) / 7e+12; // Отношения площадей поперечного сечения пучка, прошедшего щель и изначального пучка 

	// Определяем число фотонов
	N = round(N * t * k1); // Поток фотонов * время облучения * отношение площадей

	return 0;
}

int startParams() {
	// Моделируем начальную координату частицы с равномерным распределением по щели 
	x = -sR + (sL + sR) * rnd128();
	z = -sB + (sU + sB) * rnd128();
	y = 0;

	// Задаём начальный вектор скорости
	k1 = -0.01e-3 + (0.02e-3) * rnd128();
	k2 = 2 * M_PI * rnd128();
	u = k1 * cos(k2); v = cos(k1); w = k1 * sin(k2);

	// Определяем энергию фотона методом исключения
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

	return 0;
}

int cs_AbsToSamp() {
	// Переходим в СК образца
	x_moved = x - c_x;
	y_moved = y - c_y; // Положение частицы в сдвинутой в центр гониометра СК
	z_moved = z - c_z;

	x_s = R_s[0] * x_moved + R_s[1] * y_moved + R_s[2] * z_moved - s_x;
	y_s = R_s[3] * x_moved + R_s[4] * y_moved + R_s[5] * z_moved - s_y; // Положение частицы в СК образца
	z_s = R_s[6] * x_moved + R_s[7] * y_moved + R_s[8] * z_moved - s_z;

	u_s = R_s[0] * u + R_s[1] * v + R_s[2] * w;
	v_s = R_s[3] * u + R_s[4] * v + R_s[5] * w; // Скорость частицы в СК образца
	w_s = R_s[6] * u + R_s[7] * v + R_s[8] * w;

	return 0;
}

int cs_SampToDet() {
	// Переходим в СК детектора из системы координат образца
	x_moved = x_s + s_x;
	y_moved = y_s + s_y; // Положение частицы в сдвинутой в центр гониометра СК
	z_moved = z_s + s_z;

	x_d = (R_d[0] * R_s[0] + R_d[1] * R_s[1] + R_d[2] * R_s[2]) * x_moved
		+ (R_d[0] * R_s[3] + R_d[1] * R_s[4] + R_d[2] * R_s[5]) * y_moved
		+ (R_d[0] * R_s[6] + R_d[1] * R_s[7] + R_d[2] * R_s[8]) * z_moved - d_x;

	y_d = (R_d[3] * R_s[0] + R_d[4] * R_s[1] + R_d[5] * R_s[2]) * x_moved
		+ (R_d[3] * R_s[3] + R_d[4] * R_s[4] + R_d[5] * R_s[5]) * y_moved
		+ (R_d[3] * R_s[6] + R_d[4] * R_s[7] + R_d[5] * R_s[8]) * z_moved - d_y;  // Положение частицы в СК детектора

	z_d = (R_d[6] * R_s[0] + R_d[7] * R_s[1] + R_d[8] * R_s[2]) * x_moved
		+ (R_d[6] * R_s[3] + R_d[7] * R_s[4] + R_d[8] * R_s[5]) * y_moved
		+ (R_d[6] * R_s[6] + R_d[7] * R_s[7] + R_d[8] * R_s[8]) * z_moved - d_z;

	u_d = (R_d[0] * R_s[0] + R_d[1] * R_s[1] + R_d[2] * R_s[2]) * u_s
		+ (R_d[0] * R_s[3] + R_d[1] * R_s[4] + R_d[2] * R_s[5]) * v_s
		+ (R_d[0] * R_s[6] + R_d[1] * R_s[7] + R_d[2] * R_s[8]) * w_s;

	v_d = (R_d[3] * R_s[0] + R_d[4] * R_s[1] + R_d[5] * R_s[2]) * u_s
		+ (R_d[3] * R_s[3] + R_d[4] * R_s[4] + R_d[5] * R_s[5]) * v_s
		+ (R_d[3] * R_s[6] + R_d[4] * R_s[7] + R_d[5] * R_s[8]) * w_s; // Скорость частицы в СК детектора

	w_d = (R_d[6] * R_s[0] + R_d[7] * R_s[1] + R_d[8] * R_s[2]) * u_s
		+ (R_d[6] * R_s[3] + R_d[7] * R_s[4] + R_d[8] * R_s[5]) * v_s
		+ (R_d[6] * R_s[6] + R_d[7] * R_s[7] + R_d[8] * R_s[8]) * w_s;

	return 0;
}

int intersectionPoint() {
	// Определяем координату попадания фотона в образец
	intersectionFlag = 1; // Флаг наличия попадания 

	// Определяем интервал параметра t для x
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
	// Определяем интервал параметра t для y
	if (v_s == 0) {
		if (((-ySampleSize / 2 - y_s) <= 0) && (0 <= (ySampleSize / 2 - y_s))) {
			ty_l = -1e+15;
			ty_r = 1e+15;
		}
		else {
			intersectionFlag = 0;
		}
	}
	else if (v_s > 0) {
		ty_r = (ySampleSize / 2 - y_s) / v_s;
		ty_l = (-ySampleSize / 2 - y_s) / v_s;
	}
	else {
		ty_r = (-ySampleSize / 2 - y_s) / v_s;
		ty_l = (ySampleSize / 2 - y_s) / v_s;
	}
	// Определяем интервал параметра t для z
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

	// Определяем левую границу интервала параметра t для всех переменных сразу
	t_l = tx_l;
	if (ty_l > t_l) t_l = ty_l;
	if (tz_l > t_l) t_l = tz_l;
	// Определяем правую границу интервала параметра t для всех переменных сразу
	t_r = tx_r;
	if (ty_r < t_r) t_r = ty_r;
	if (tz_r < t_r) t_r = tz_r;

	// Определяем координаты попадания фотона в образец
	if (t_l > t_r) intersectionFlag = 0;
	else {
		x_s += t_l * u_s;
		y_s += t_l * v_s;
		z_s += t_l * w_s;
	}
	
	return 0;
}

int imgOnDet() {
	// Формируем изображение на детекторе
	if (v_d > 0) { // Так как детектор расположен в плоскости Ox_d,z_d, это услоиве вполне отсекает фотоны, летящее от него
		t_l = -y_d / v_d; // Параметр, описывающий точку пересечения луча фотона и плоскости детектора
		x_d += u_d * t_l;
		z_d += w_d * t_l;

		if ((fabs(x_d) <= 84.968e+6) && (fabs(z_d) <= 84.968e+6)) { // Условие попадания в детектор
			det1 = (int)((84.968e+6 - x_d) / 172e+3); // Координаты зажжёного пикселя
			det2 = (int)((84.968e+6 - z_d) / 172e+3);
			detArray[det1][det2] += E;
			max_dA = (max_dA > detArray[det1][det2]) ? max_dA : detArray[det1][det2];
			min_dA = (min_dA < detArray[det1][det2] && detArray[det1][det2] != 0) ? min_dA : detArray[det1][det2];
			detArray_squared[det1][det2] += E * E;
		}
	}

	return 0;
}