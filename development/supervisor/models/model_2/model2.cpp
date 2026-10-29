#include <string>
#include <functional>
#include <fstream>
#include <iostream>
#include <thread>
#include <chrono>
#include <filesystem>
#include <csignal>
#include <atomic>
#include <sstream>
#include <iomanip>
#include <openssl/sha.h>

#include "./json/include/nlohmann/json.hpp"

using json = nlohmann::json;


std::atomic<bool> g_running{true};

void signalHandler(int signum)
{
    std::cout << "\nreceived signal " << signum << ", shutting down..." << std::endl;
    g_running = false;
}


// простая функция для хеширования содержимого файла
std::string computeFileHash(const std::string& path)
{
    std::ifstream file(path, std::ios::binary);
    if(!file.is_open())
        return "";

    std::ostringstream ss;
    ss << file.rdbuf();
    std::string content = ss.str();

    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256(reinterpret_cast<const unsigned char*>(content.c_str()),
           content.size(), hash);

    std::ostringstream hex;
    for(int i = 0; i < SHA256_DIGEST_LENGTH; ++i)
        hex << std::hex << std::setw(2) << std::setfill('0')
            << static_cast<int>(hash[i]);

    return hex.str();
}


class ModelRunner
{
public:

    ModelRunner(
        const std::string& inputFile,
        const std::string& outputFile,
        std::function<double(double,double)> calculation
    );


    void run();


private:

    std::string inputFile;
    std::string outputFile;

    std::function<double(double,double)> calculation;


    long lastStep = -1;
    std::string lastFileHash;


    bool readInput(
        long& step,
        double& x0,
        double& y
    );


    void writeResult(
        long step,
        double value
    );
};


ModelRunner::ModelRunner(
    const std::string& inputFile,
    const std::string& outputFile,
    std::function<double(double,double)> calculation
)
:
inputFile(inputFile),
outputFile(outputFile),
calculation(calculation)
{

}



bool ModelRunner::readInput(
    long& step,
    double& x0,
    double& y
)
{

    std::ifstream file(inputFile);


    if(!file.is_open())
    {
        std::cout << "[log] input file not found: " << inputFile << std::endl;
        return false;
    }


    json j;

    try
    {
        file >> j;
    }
    catch(const std::exception& e)
    {
        std::cout << "[log] failed to parse json: " << e.what() << std::endl;
        return false;
    }
    catch(...)
    {
        std::cout << "[log] unknown error while parsing json" << std::endl;
        return false;
    }


    step = j["step"];
    x0   = j["x0"];
    y    = j["y"];


    return true;
}

void ModelRunner::writeResult(
    long step,
    double value
)
{

    json result;


    result["step"] = step;
    result["value"] = value;

    std::string tmp = outputFile + ".tmp";

    {
        std::ofstream file(tmp);

        file << result.dump(4);
    }

    std::filesystem::rename(
        tmp,
        outputFile
    );

    std::cout
        << "[log] result written step="
        << step
        << " value="
        << value
        << std::endl;
}

void ModelRunner::run()
{

    std::cout
        << "[log] model started, watching: " << inputFile
        << std::endl;


    while(g_running)
    {

        // --- проверим, существует ли файл ---
        if(!std::filesystem::exists(inputFile))
        {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            continue;
        }


        // --- посчитаем хеш содержимого файла ---
        std::string currentHash = computeFileHash(inputFile);

        if(currentHash.empty())
        {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            continue;
        }


        // --- сравним с предыдущим хешем ---
        if(currentHash == lastFileHash)
        {
            // файл не менялся
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            continue;
        }


        std::cout << "[log] file changed, new hash: " << currentHash.substr(0, 16) << "..." << std::endl;

        lastFileHash = currentHash;


        long step;
        double x0;
        double y;



        if(readInput(step, x0, y))
        {

            std::cout
                << "[log] new data received: step=" << step
                << " x0=" << x0
                << " y=" << y
                << std::endl;


            if(step != lastStep)
            {

                std::cout
                    << "[log] new step "
                    << step
                    << std::endl;


                lastStep = step;



                // имитация тяжелого расчета
                std::this_thread::sleep_for(
                    std::chrono::seconds(3)
                );



                double result =
                    calculation(x0, y);



                writeResult(
                    step,
                    result
                );

            }
            else
            {
                std::cout
                    << "[log] same step " << step << " but different data, recalculating"
                    << std::endl;

                // если хочешь пересчитывать даже при том же step — убери проверку выше
                // или раскомментируй этот блок

                std::this_thread::sleep_for(
                    std::chrono::seconds(3)
                );

                double result =
                    calculation(x0, y);

                writeResult(
                    step,
                    result
                );

            }

        }
        else
        {
            std::cout << "[log] readInput failed, will retry next iteration" << std::endl;
        }

    }


    std::cout << "[log] model stopped" << std::endl;

}

std::string getInputFile()
{
    const char* env = std::getenv("INPUT_FILE");
    return env ? env : "/data/start2.json";
}

std::string getOutputFile()
{
    const char* env = std::getenv("OUTPUT_FILE");
    return env ? env : "/data/end2.json";
}

int main()
{
    std::signal(SIGINT,  signalHandler);
    std::signal(SIGTERM, signalHandler);

    std::signal(SIGINT,  signalHandler);
    std::signal(SIGTERM, signalHandler);

    ModelRunner model(
        getInputFile(),
        getOutputFile(),
         [](double x0, double y)
        {
            return x0 * y * y;
        }
    );

    model.run();
    return 0;
}