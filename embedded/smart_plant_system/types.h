#ifndef TYPES_H
#define TYPES_H

#include <Arduino.h>

struct BackendCommand {
    bool hasCommand;
    String action;
    String reason;
    int duration;    
};

#endif